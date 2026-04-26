package com.kkamangnya.remoteon

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kkamangnya.remoteon.databinding.ActivityMainBinding
import com.kkamangnya.remoteon.databinding.DialogRemotePcBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RemotePcAdapter
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(PcRepository(PcStore(applicationContext)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RemotePcAdapter(
            onWake = viewModel::wakePc,
            onPing = viewModel::pingPc,
            onDelete = viewModel::deletePc,
            onEdit = { showPcDialog(it) }
        )

        binding.pcRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.pcRecyclerView.adapter = adapter

        binding.addPcButton.setOnClickListener { showPcDialog() }
        binding.refreshButton.setOnClickListener { viewModel.refresh() }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.pcs)
                binding.emptyStateText.isVisible = state.pcs.isEmpty() && !state.isLoading
                binding.loadingText.isVisible = state.isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.events.collectLatest { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPcDialog(pc: RemotePc? = null) {
        val dialogBinding = DialogRemotePcBinding.inflate(LayoutInflater.from(this))
        dialogBinding.nameInput.setText(pc?.name.orEmpty())
        dialogBinding.macInput.setText(pc?.macAddress.orEmpty())
        dialogBinding.ipInput.setText(pc?.ipAddress.orEmpty())
        dialogBinding.broadcastInput.setText(pc?.broadcastAddress.orEmpty())

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (pc == null) "PC 추가" else "PC 수정")
            .setView(dialogBinding.root)
            .setPositiveButton("저장", null)
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.nameInput.text?.toString()?.trim().orEmpty()
                val mac = dialogBinding.macInput.text?.toString()?.trim().orEmpty()
                val ip = dialogBinding.ipInput.text?.toString()?.trim().orEmpty()
                val broadcast = dialogBinding.broadcastInput.text?.toString()?.trim().orEmpty()

                if (name.isBlank() || mac.isBlank() || ip.isBlank() || broadcast.isBlank()) {
                    Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val normalizedMac = mac.replace(":", "").replace("-", "").replace(" ", "")
                if (normalizedMac.length != 12 || normalizedMac.any { it !in "0123456789abcdefABCDEF" }) {
                    Toast.makeText(this, "MAC 주소 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.savePc(
                    (pc ?: RemotePc(name = name, macAddress = mac, ipAddress = ip, broadcastAddress = broadcast))
                        .copy(
                            name = name,
                            macAddress = mac,
                            ipAddress = ip,
                            broadcastAddress = broadcast
                        )
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
