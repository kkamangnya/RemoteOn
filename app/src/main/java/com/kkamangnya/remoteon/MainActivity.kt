package com.kkamangnya.remoteon

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.loadNightMode(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RemotePcAdapter(
            onWake = viewModel::wakePc,
            onPing = viewModel::pingPc,
            onDelete = viewModel::deletePc,
            onEdit = { showPcDialog(it) }
        )

        bindThemeSwitch()

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
        dialogBinding.macInput.setText(formatMacAddress(pc?.macAddress.orEmpty()))
        dialogBinding.ipInput.setText(pc?.ipAddress.orEmpty())
        dialogBinding.subnetInput.setText(pc?.subnetMask.orEmpty())
        dialogBinding.broadcastInput.setText(pc?.broadcastAddress.orEmpty())

        var selfUpdatingMac = false
        dialogBinding.macInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (selfUpdatingMac) return
                val raw = s?.toString().orEmpty()
                val formatted = formatMacAddress(raw)
                if (formatted == raw) return
                selfUpdatingMac = true
                dialogBinding.macInput.setText(formatted)
                dialogBinding.macInput.setSelection(formatted.length)
                selfUpdatingMac = false
            }
        })

        var selfUpdatingBroadcast = false
        val autoBroadcastUpdater = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (selfUpdatingBroadcast) return
                val ip = dialogBinding.ipInput.text?.toString().orEmpty()
                val subnet = dialogBinding.subnetInput.text?.toString().orEmpty()
                val computed = NetworkTools.calculateBroadcastAddress(ip, subnet)
                if (computed != null) {
                    selfUpdatingBroadcast = true
                    dialogBinding.broadcastInput.setText(computed)
                    dialogBinding.broadcastInput.setSelection(computed.length)
                    selfUpdatingBroadcast = false
                }
            }
        }
        dialogBinding.ipInput.addTextChangedListener(autoBroadcastUpdater)
        dialogBinding.subnetInput.addTextChangedListener(autoBroadcastUpdater)

        val initialBroadcast = NetworkTools.calculateBroadcastAddress(
            dialogBinding.ipInput.text?.toString().orEmpty(),
            dialogBinding.subnetInput.text?.toString().orEmpty()
        )
        if (initialBroadcast != null) {
            dialogBinding.broadcastInput.setText(initialBroadcast)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (pc == null) "PC 추가" else "PC 수정")
            .setView(dialogBinding.root)
            .setPositiveButton("저장", null)
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.nameInput.text?.toString()?.trim().orEmpty()
                val mac = dialogBinding.macInput.text?.toString()?.trim().orEmpty()
                val ip = dialogBinding.ipInput.text?.toString()?.trim().orEmpty()
                val subnet = dialogBinding.subnetInput.text?.toString()?.trim().orEmpty()
                val broadcast = dialogBinding.broadcastInput.text?.toString()?.trim().orEmpty()

                if (name.isBlank() || mac.isBlank() || ip.isBlank() || subnet.isBlank() || broadcast.isBlank()) {
                    Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val normalizedMac = normalizeMacAddress(mac)
                if (normalizedMac.length != 12 || normalizedMac.any { it !in "0123456789ABCDEF" }) {
                    Toast.makeText(this, "MAC 주소 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!NetworkTools.isValidIpv4(ip)) {
                    Toast.makeText(this, "IP 주소 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!NetworkTools.isValidIpv4(subnet)) {
                    Toast.makeText(this, "서브넷 마스크 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val computedBroadcast = NetworkTools.calculateBroadcastAddress(ip, subnet)
                if (computedBroadcast == null) {
                    Toast.makeText(this, "브로드캐스트 주소를 계산할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val formattedMac = formatMacAddress(normalizedMac)
                viewModel.savePc(
                    (pc ?: RemotePc(
                        name = name,
                        macAddress = formattedMac,
                        ipAddress = ip,
                        subnetMask = subnet,
                        broadcastAddress = computedBroadcast
                    )).copy(
                        name = name,
                        macAddress = formattedMac,
                        ipAddress = ip,
                        subnetMask = subnet,
                        broadcastAddress = computedBroadcast
                    )
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun bindThemeSwitch() {
        val currentMode = ThemePrefs.loadNightMode(this)
        binding.themeSwitch.isChecked = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemInNightMode()
        }
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val selectedMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            ThemePrefs.saveNightMode(this, selectedMode)
            AppCompatDelegate.setDefaultNightMode(selectedMode)
        }
    }

    private fun isSystemInNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun normalizeMacAddress(value: String): String {
        return value.replace(":", "")
            .replace("-", "")
            .replace(" ", "")
            .uppercase()
    }

    private fun formatMacAddress(value: String): String {
        val normalized = normalizeMacAddress(value)
        if (normalized.isBlank()) return ""
        return normalized.chunked(2).take(6).joinToString("-")
    }
}
