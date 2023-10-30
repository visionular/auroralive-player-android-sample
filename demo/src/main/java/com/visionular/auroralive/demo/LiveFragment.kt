package com.visionular.auroralive.demo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.visionular.auroralive.AuroraLivePlayer
import com.visionular.auroralive.demo.databinding.LiveFragBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LiveFragment : Fragment(), AuroraLivePlayer.Listener, AdapterView.OnItemSelectedListener {

    private var _binding: LiveFragBinding? = null
    private val _args: LiveFragmentArgs by navArgs()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var auroraLive: AuroraLivePlayer
    private lateinit var playbackId: String
    private lateinit var statsTimer: Timer
    private var lastBytesRecv: Long = 0

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = LiveFragBinding.inflate(inflater, container, false)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) {
            return
        }

        playbackId = _args.playbackId
        val token = _args.token
        auroraLive = AuroraLivePlayer(context!!.applicationContext, this).apply {
            renderer = binding.videoView
        }

        binding.versionText.text = "version: ${AuroraLivePlayer.VERSION}"
        binding.layerSpinner.onItemSelectedListener = this

        lifecycleScope.launch {
            auroraLive.play(playbackId, token)
        }

//        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_LiveFragment_to_EntryLiveFragment)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::statsTimer.isInitialized) {
            statsTimer.cancel()
        }

        auroraLive.close()
        lastBytesRecv = 0

        _binding = null
    }

    override fun onPlayerError(msg: String) {
        Toast.makeText(this.context, msg, Toast.LENGTH_LONG).show()
    }

    override fun onPlaybackSuccess(streamInfo: AuroraLivePlayer.StreamInfo) {
        binding.videoView.visibility = View.VISIBLE
        var videoLayersInfo = streamInfo.videoLayersInfo
        val layerList = ArrayList<AuroraLivePlayer.AuroraLiveVideoLayer>()
        if(videoLayersInfo != null){
            var currentPos = videoLayersInfo.current;
            videoLayersInfo.layers.forEachIndexed{ index, item ->
                layerList.add(AuroraLivePlayer.AuroraLiveVideoLayer("${item.width}"+"x"+"${item.height}", item.rid))
            }
            binding.layerSpinner.adapter = ArrayAdapter(context!!, R.layout.layer_spinner_item, layerList)
            binding.layerSpinner.setSelection(currentPos)
        }else{
            binding.layerSpinner.adapter = ArrayAdapter(context!!, R.layout.layer_spinner_item, listOf(AuroraLivePlayer.AuroraLiveVideoLayer("unknown", "u")))
        }
        binding.layerSpinner.visibility = View.VISIBLE

        statsTimer = Timer()
        statsTimer.schedule(object: TimerTask() {
            override fun run() {
                auroraLive.requestGetStats()
            } }, 1000, 1000)
        Toast.makeText(this.context, "onPlaybackSuccess", Toast.LENGTH_SHORT).show()
    }

    override fun onSwitchLayerSuccess() {
        Toast.makeText(this.context, "switch layer success", Toast.LENGTH_SHORT).show()
    }

    override fun onSwitchLayerFailed(msg: String) {
        Toast.makeText(this.context, "switch layer error: $msg", Toast.LENGTH_LONG).show()
    }

    override fun onRequestStats(stats: AuroraLivePlayer.Stats) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.pktLostText.text = "pkt lost: ${stats.videoPacketsLost + stats.audioPacketsLost}"
            binding.sizeText.text = "size: ${stats.videoWidth}x${stats.videoHeight}"
            //binding.fpsText.text = "fps: ${stats.videoFps}"
            binding.bitrateText.text = "bitrate: ${(stats.audioBytesReceived + stats.videoBytesReceived - lastBytesRecv) * 8 / 1000 }kb/s"
            lastBytesRecv = stats.audioBytesReceived + stats.videoBytesReceived
        }
    }

    override fun onRequestStatsError(msg: String) {
        Toast.makeText(this.context, "get stats error: $msg", Toast.LENGTH_LONG).show()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val layer = parent.getItemAtPosition(pos)
        var videoLayer = layer as AuroraLivePlayer.AuroraLiveVideoLayer
        if(videoLayer.desc == "unknown"){
            return
        }
        lifecycleScope.launch {
            auroraLive.layer(videoLayer)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }
}