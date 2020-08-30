package com.kylecorry.trail_sense.navigation.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.*
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.Flashlight
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.FlashlightService
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.SosService
import com.kylecorry.trail_sense.navigation.infrastructure.*
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.switchToFragment
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.ceil
import kotlin.math.roundToInt

class NavigatorFragment(
    private val initialDestination: Beacon? = null,
    private val createBeacon: GeoUriParser.NamedCoordinate? = null
) : Fragment() {

    constructor() : this(null, null)

    private lateinit var compass: ICompass
    private lateinit var gps: IGPS
    private lateinit var declinationProvider: IDeclinationProvider
    private lateinit var orientation: DeviceOrientation
    private lateinit var altimeter: IAltimeter
    private val throttle = Throttle(20)

    // TODO: Extract ruler
    private var isRulerSetup = false
    private var areRulerTextViewsAligned = false

    private lateinit var userPrefs: UserPreferences

    private lateinit var navigationVM: NavigationViewModel

    private lateinit var locationTxt: TextView
    private lateinit var altitudeTxt: TextView
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var rulerBtn: FloatingActionButton
    private lateinit var flashlightBtn: FloatingActionButton
    private lateinit var ruler: ConstraintLayout
    private lateinit var parentLayout: ConstraintLayout
    private lateinit var accuracyView: LinearLayout
    private lateinit var gpsAccuracyTxt: TextView
    private lateinit var compassAccuracyTxt: TextView
    private lateinit var gpsAccuracy: LinearLayout
    private lateinit var compassAccuracy: LinearLayout
    private lateinit var speedTxt: TextView
    private lateinit var destinationPanel: DestinationPanel
    private lateinit var beaconIndicators: List<ImageView>
    private lateinit var visibleCompass: ICompassView
    private lateinit var roundCompass: ICompassView
    private lateinit var linearCompass: ICompassView

    private lateinit var beaconRepo: BeaconRepo
    private var flashlightState = FlashlightState.Off

    private val sensorService by lazy { SensorService(requireContext()) }
    private val navigationService = NavigationService()

    private var timer: Timer? = null
    private var handler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        if (createBeacon != null) {
            switchToFragment(PlaceBeaconFragment(createBeacon), addToBackStack = true)
        }

        bindUI(view)

        beaconRepo = BeaconRepo(requireContext())
        compass = sensorService.getCompass()
        orientation = sensorService.getDeviceOrientation()
        gps = sensorService.getGPS()
        declinationProvider = sensorService.getDeclinationProvider()
        altimeter = sensorService.getAltimeter()

        navigationVM =
            NavigationViewModel(compass, gps, altimeter, orientation, userPrefs, beaconRepo)
        navigationVM.beacon = initialDestination

        updateUI()
        return view
    }

    private fun bindUI(view: View){
        userPrefs = UserPreferences(requireContext())
        locationTxt = view.findViewById(R.id.location)
        altitudeTxt = view.findViewById(R.id.altitude)
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        rulerBtn = view.findViewById(R.id.ruler_btn)
        flashlightBtn = view.findViewById(R.id.flashlight_btn)
        ruler = view.findViewById(R.id.ruler)
        parentLayout = view.findViewById(R.id.navigator_layout)
        accuracyView = view.findViewById(R.id.accuracy_view)
        gpsAccuracyTxt = view.findViewById(R.id.gps_accuracy_text)
        compassAccuracyTxt = view.findViewById(R.id.compass_accuracy_text)
        gpsAccuracy = view.findViewById(R.id.gps_accuracy_view)
        compassAccuracy = view.findViewById(R.id.compass_accuracy_view)
        speedTxt = view.findViewById(R.id.speed)

        destinationPanel = DestinationPanel(view.findViewById(R.id.navigation_sheet))

        val beacons = mutableListOf<ImageView>()

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 3)) {
            beacons.add(ImageView(requireContext()))
        }
        beaconIndicators = beacons

        val astronomyColor = UiUtils.androidTextColorPrimary(requireContext())

        val arrowImg = resources.getDrawable(R.drawable.ic_arrow_target, null)
        val sunImg = resources.getDrawable(R.drawable.sun, null)
        sunImg.setTint(astronomyColor)

        val moonImg = resources.getDrawable(R.drawable.moon_waxing_crescent, null)
        moonImg.setTint(astronomyColor)

        beaconIndicators.forEach {
            it.setImageDrawable(arrowImg)
            it.visibility = View.INVISIBLE
            parentLayout.addView(it)
        }

        beaconIndicators[0].setImageDrawable(sunImg)
        beaconIndicators[1].setImageDrawable(moonImg)

        roundCompass = CompassView(
            view.findViewById(R.id.needle),
            beaconIndicators,
            view.findViewById(R.id.azimuth_indicator)
        )
        linearCompass = LinearCompassViewHldr(
            view.findViewById(R.id.linear_compass),
            beaconIndicators
        )

        visibleCompass = roundCompass

        locationTxt.setOnLongClickListener {
            val sender = LocationSharesheet(requireContext())
            sender.send(navigationVM.shareableLocation)
            true
        }

        beaconBtn.setOnClickListener {
            if (!navigationVM.showDestination) {
                switchToFragment(
                    BeaconListFragment(beaconRepo),
                    addToBackStack = true
                )
            } else {
                navigationVM.beacon = null
                updateNavigator()
            }
        }

        rulerBtn.setOnClickListener {
            if (ruler.visibility == View.VISIBLE) {
                rulerBtn.imageTintList =
                    ColorStateList.valueOf(UiUtils.androidTextColorSecondary(requireContext()))
                rulerBtn.backgroundTintList =
                    ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(requireContext()))
                ruler.visibility = View.GONE
            } else {
                rulerBtn.imageTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorSecondary, null))
                rulerBtn.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorPrimary, null))
                ruler.visibility = View.VISIBLE
            }
        }

        if (!Flashlight.hasFlashlight(requireContext())) {
            flashlightBtn.visibility = View.GONE
        }

        flashlightBtn.setOnClickListener {
            flashlightState = getNextFlashlightState(flashlightState)
            when (flashlightState) {
                FlashlightState.On -> {
                    SosService.stop(requireContext().applicationContext)
                    FlashlightService.start(requireContext().applicationContext)
                }
                FlashlightState.SOS -> {
                    FlashlightService.stop(requireContext().applicationContext)
                    SosService.start(requireContext().applicationContext)
                }
                FlashlightState.Off -> {
                    SosService.stop(requireContext().applicationContext)
                    FlashlightService.stop(requireContext().applicationContext)
                }
            }
        }

        accuracyView.setOnClickListener {
            UiUtils.alert(
                requireContext(),
                getString(R.string.accuracy_info_title),
                getString(
                    R.string.accuracy_info,
                    navigationVM.gpsHorizontalAccuracy,
                    navigationVM.gpsVerticalAccuracy,
                    navigationVM.gpsSatellites
                )
            )
        }
    }


    private fun getFlashlightState(): FlashlightState {
        return when {
            FlashlightService.isOn(requireContext()) -> FlashlightState.On
            SosService.isOn(requireContext()) -> FlashlightState.SOS
            else -> FlashlightState.Off
        }
    }

    private fun getNextFlashlightState(currentState: FlashlightState): FlashlightState {
        return when (currentState) {
            FlashlightState.On -> FlashlightState.SOS
            FlashlightState.SOS -> FlashlightState.Off
            FlashlightState.Off -> FlashlightState.On
        }
    }

    private fun updateFlashlight(flashlightState: FlashlightState) {
        when (flashlightState) {
            FlashlightState.On -> {
                flashlightBtn.setImageResource(R.drawable.flashlight)
                flashlightBtn.imageTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorSecondary, null))
                flashlightBtn.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorPrimary, null))
            }
            FlashlightState.SOS -> {
                flashlightBtn.imageTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorSecondary, null))
                flashlightBtn.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorPrimary, null))
                flashlightBtn.setImageResource(R.drawable.flashlight_sos)
            }
            else -> {
                flashlightBtn.setImageResource(R.drawable.flashlight)
                flashlightBtn.imageTintList =
                    ColorStateList.valueOf(UiUtils.androidTextColorSecondary(requireContext()))
                flashlightBtn.backgroundTintList =
                    ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(requireContext()))
            }
        }
    }

    private fun setVisibleCompass(compass: ICompassView) {
        if (visibleCompass == compass) {
            if (compass != roundCompass) {
                roundCompass.visibility = View.INVISIBLE
            } else {
                linearCompass.visibility = View.INVISIBLE
            }
        }

        compass.beacons = navigationVM.nearestBeacons
        compass.azimuth = navigationVM.azimuth
        visibleCompass.visibility = View.INVISIBLE
        visibleCompass = compass
        visibleCompass.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        flashlightState = getFlashlightState()
        compass.start(this::onCompassUpdate)
        gps.start(this::onLocationUpdate)
        altimeter.start(this::onAltitudeUpdate)
        orientation.start(this::onOrientationUpdate)

        if (declinationProvider.hasValidReading) {
            onDeclinationUpdate()
        } else {
            declinationProvider.start(this::onDeclinationUpdate)
        }

        val hasGPS = SensorChecker(requireContext()).hasGPS()

        if (!hasGPS) {
            beaconBtn.hide()
        } else {
            beaconBtn.show()
            if (userPrefs.navigation.showMultipleBeacons) {
                val that = this
                handler = Handler(Looper.getMainLooper())
                timer = fixedRateTimer(period = 15000) {
                    handler?.post {
                        gps.start(that::onLocationUpdate)
                    }
                }
            }
        }

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        compass.stop(this::onCompassUpdate)
        gps.stop(this::onLocationUpdate)
        altimeter.stop(this::onAltitudeUpdate)
        orientation.stop(this::onOrientationUpdate)
        declinationProvider.stop(this::onDeclinationUpdate)
        timer?.cancel()
        timer = null
    }

    private fun updateUI() {

        if (throttle.isThrottled() || context == null) {
            return
        }

        val position = getPosition()
        val destination = getDestination()

        updateFlashlight(flashlightState)
        updateDestination(position, destination)
        updateAccuracy(compass.accuracy, gps.accuracy, gps.verticalAccuracy, gps.horizontalAccuracy, gps.satellites)
        updateSpeed(position.speed)
        updateCompassView() // TODO: Pass in needed values
        updateRuler()
        updateAzimuth(position.bearing)
        updateAltitude(position.elevation)
        updateCoordinates(position.location)
    }

    private fun updateAccuracy(compassAccuracy: Accuracy, gpsAccuracy: Accuracy, gpsVerticalAccuracy: Float?, gpsHorizontalAccuracy: Float?, satellites: Int) {
        // TODO: Use these values
        gpsAccuracyTxt.text = navigationVM.gpsAccuracy
        compassAccuracyTxt.text = navigationVM.compassAccuracy

        if (navigationVM.showCompassAccuracy) {
            this.compassAccuracy.visibility = View.VISIBLE
        } else {
            this.compassAccuracy.visibility = View.INVISIBLE
        }

        if (navigationVM.showGpsAccuracy) {
            this.gpsAccuracy.visibility = View.VISIBLE
        } else {
            this.gpsAccuracy.visibility = View.INVISIBLE
        }
    }

    private fun updateAltitude(altitude: Float){
        altitudeTxt.text = if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            "${altitude.roundToInt()} m"
        } else {
            "${LocationMath.convertToBaseUnit(altitude, UserPreferences.DistanceUnits.Feet)
                .roundToInt()} ft"
        }
    }

    private fun updateCoordinates(location: Coordinate){
        locationTxt.text = userPrefs.navigation.formatLocation(location)
    }

    private fun updateSpeed(speed: Float){
        speedTxt.text = getString(
            R.string.speed_format,
            LocationMath.convertToBaseSpeed(speed, userPrefs.distanceUnits).roundPlaces(1)
                .toString(),
            getString(navigationVM.speedUnit)
        )

    }

    private fun updateAzimuth(azimuth: Bearing){
        azimuthTxt.text = getString(R.string.degree_format, azimuth.value.roundToInt())
        directionTxt.text = azimuth.direction.symbol
    }

    private fun onOrientationUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onCompassUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onDeclinationUpdate(): Boolean {
        navigationVM.declination = declinationProvider.declination
        updateUI()
        return false
    }

    private fun onAltitudeUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onLocationUpdate(): Boolean {
        updateUI()
        navigationVM.onLocationUpdate()
        return navigationVM.showDestination
    }

    private fun updateRuler() {
        val dpi = resources.displayMetrics.densityDpi
        val height =
            navigationVM.rulerScale * ruler.height / dpi.toDouble() * if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters) 2.54 else 1.0

        if (height == 0.0 || context == null) {
            return
        }

        if (!isRulerSetup) {
            val primaryColor = UiUtils.androidTextColorPrimary(requireContext())

            for (i in 0..ceil(height).toInt() * 8) {
                val inches = i / 8.0
                val tv = TextView(context)
                val bar = View(context)
                bar.setBackgroundColor(primaryColor)
                val layoutParams = ConstraintLayout.LayoutParams(1, 4)
                bar.layoutParams = layoutParams
                when {
                    inches % 1.0 == 0.0 -> {
                        bar.layoutParams.width = 48
                        tv.text = inches.toInt().toString()
                    }
                    inches % 0.5 == 0.0 -> {
                        bar.layoutParams.width = 36
                    }
                    inches % 0.25 == 0.0 -> {
                        bar.layoutParams.width = 24
                    }
                    else -> {
                        bar.layoutParams.width = 12
                    }
                }
                bar.y =
                    ruler.height * (inches / height).toFloat() + resources.getDimensionPixelSize(R.dimen.ruler_top)
                if (!tv.text.isNullOrBlank()) {
                    tv.setTextColor(primaryColor)
                    ruler.addView(tv)
                    tv.y = bar.y
                    tv.x =
                        bar.layoutParams.width.toFloat() + resources.getDimensionPixelSize(R.dimen.ruler_label)
                }

                ruler.addView(bar)
            }
        } else if (!areRulerTextViewsAligned) {
            for (view in ruler.children) {
                if (view.height != 0) {
                    areRulerTextViewsAligned = true
                }
                view.y -= view.height / 2f
            }
        }

        isRulerSetup = true
    }

    private fun updateNavigator() {
        if (isNavigating()) {
            // Navigating
            gps.start(this::onLocationUpdate)
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_cancel))
            onLocationUpdate()
        } else {
            // Not navigating
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_beacon))
            onLocationUpdate()
        }
    }


    private fun updateDestination(position: Position, destination: Beacon?) {
        if (destination == null) {
            destinationPanel.hide()
            return
        }

        destinationPanel.show(position, destination)
    }

    private fun updateCompassView(){
        setVisibleCompass(getCompassView())
        beaconIndicators[0].visibility = navigationVM.sunBeaconVisibility
        beaconIndicators[1].visibility = navigationVM.moonBeaconVisibility
        beaconIndicators[0].alpha = navigationVM.sunBeaconOpacity
        beaconIndicators[1].alpha = navigationVM.moonBeaconOpacity

        beaconIndicators.forEach {
            if (it.height == 0) {
                it.visibility = View.INVISIBLE
            }
        }
    }

    private fun getCompassView(): ICompassView {
        return if (navigationVM.showLinearCompass) {
            linearCompass
        } else {
            roundCompass
        }
    }

    private fun isNavigating(): Boolean {
        return navigationVM.showDestination
    }

    private fun getDestination(): Beacon? {
        navigationVM.updateVisibleBeacon()
        return navigationVM.visibleBeacon
    }

    private fun getPosition(): Position {
        return Position(gps.location, altimeter.altitude, compass.bearing, declinationProvider.declination, gps.speed)
    }


}
