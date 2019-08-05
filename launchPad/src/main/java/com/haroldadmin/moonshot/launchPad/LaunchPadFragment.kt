package com.haroldadmin.moonshot.launchPad

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.haroldadmin.moonshot.LaunchTypes
import com.haroldadmin.moonshot.MainViewModel
import com.haroldadmin.moonshot.R as appR
import com.haroldadmin.moonshot.base.MoonShotFragment
import com.haroldadmin.moonshot.base.asyncTypedEpoxyController
import com.haroldadmin.moonshot.core.Resource
import com.haroldadmin.moonshot.itemError
import com.haroldadmin.moonshot.itemExpandableTextWithHeading
import com.haroldadmin.moonshot.itemLaunchDetail
import com.haroldadmin.moonshot.itemLoading
import com.haroldadmin.moonshot.itemTextHeader
import com.haroldadmin.moonshot.itemTextWithHeading
import com.haroldadmin.moonshot.launchPad.databinding.FragmentLaunchpadBinding
import com.haroldadmin.moonshot.models.launchpad.LaunchPad
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class LaunchPadFragment : MoonShotFragment() {

    private lateinit var binding: FragmentLaunchpadBinding
    private val safeArgs by navArgs<LaunchPadFragmentArgs>()
    private val differ by inject<Handler>(named("differ"))
    private val builder by inject<Handler>(named("builder"))
    private val viewModel by viewModel<LaunchPadViewModel> {
        val initialState = LaunchPadState(safeArgs.siteId)
        parametersOf(initialState)
    }
    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Launchpad.init()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLaunchpadBinding.inflate(inflater, container, false)
        val animation = AnimationUtils.loadLayoutAnimation(requireContext(), appR.anim.layout_animation_fade_in)
        binding.rvLaunchPad.apply {
            setController(epoxyController)
            layoutAnimation = animation
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fragmentScope.launch {
            viewModel.state.collect {
                renderState(it) { state ->
                    epoxyController.setData(state)
                    if (state.launchPad is Resource.Success) {
                        mainViewModel.setTitle(state.launchPad.data.siteNameLong)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        epoxyController.cancelPendingModelBuild()
    }

    private val epoxyController by lazy {
        asyncTypedEpoxyController(builder, differ, viewModel) { state ->
            when (val launchpad = state.launchPad) {
                is Resource.Success -> buildLaunchPadModels(this, launchpad.data)
                is Resource.Error<LaunchPad, *> -> {
                    itemTextHeader {
                        id("map")
                        header(getString(R.string.itemMapCardMapHeader))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }
                    itemError {
                        id("launchpad-error")
                        error(getString(R.string.fragmentLaunchPadLaunchPadErrorMessage))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }

                    launchpad.data?.let { lp -> buildLaunchPadModels(this, lp) }
                }
                else -> itemLoading {
                    id("launchpad-loading")
                    message(getString(R.string.fragmentLaunchPadLaunchPadLoadingMessage))
                    spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                }
            }
        }
    }

    private fun buildLaunchPadModels(controller: EpoxyController, launchpad: LaunchPad) = with(controller) {
        itemLaunchDetail {
            id("launch-pad")
            detailHeader(getString(R.string.fragmentLaunchPadLaunchPadHeader))
            detailName(launchpad.siteNameLong)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
        }
        itemExpandableTextWithHeading {
            id("launch-pad-detail")
            heading(getString(R.string.fragmentLaunchPadDetailsHeader))
            text(launchpad.details)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
        }
        itemTextWithHeading {
            id("status")
            heading(getString(R.string.fragmentLaunchPadStatusHeader))
            text(launchpad.status.capitalize())
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / 2 }
        }
        itemTextWithHeading {
            id("success-percentage")
            heading("Success Rate")
            text(launchpad.successPercentage)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / 2 }
            onTextWithDetailClick { _ ->
                LaunchPadFragmentDirections.launchPadLaunches(
                    type = LaunchTypes.LAUNCHPAD,
                    siteId = launchpad.siteId
                ).also { action ->
                    findNavController().navigate(action)
                }
            }
        }
        itemTextHeader {
            id("map")
            header(getString(R.string.itemMapCardMapHeader))
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
        }
        itemMapCard {
            id("map")
            mapImageUrl(launchpad.location.getStaticMapUrl())
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            onMapClick { _ ->
                val mapIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:${launchpad.location.latitude},${launchpad.location.longitude}")
                }
                startActivity(mapIntent)
            }
        }
    }
}