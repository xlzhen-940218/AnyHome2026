package com.draco.anyhome.views

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.anyhome.R
import com.draco.anyhome.recyclers.RecyclerEdgeEffectFactory
import com.draco.anyhome.recyclers.SelectRecyclerAdapter
import com.draco.anyhome.viewmodels.SelectActivityViewModel
import com.google.android.material.button.MaterialButton
import java.util.Locale

class SelectActivity : AppCompatActivity() {
    private val viewModel: SelectActivityViewModel by viewModels()
    private lateinit var recycler: RecyclerView
    private lateinit var recyclerAdapter: SelectRecyclerAdapter
    private lateinit var searchInput: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var appCountText: TextView
    private lateinit var emptyState: View
    private lateinit var loadingLayout: View
    private lateinit var btnReset: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var headerContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        initViews()
        setupWindowInsets()
        setupRecyclerView()
        setupSearch()
        setupActions()
        observeViewModel()
    }

    private fun initViews() {
        recycler = findViewById(R.id.recycler)
        searchInput = findViewById(R.id.searchInput)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        appCountText = findViewById(R.id.appCountText)
        emptyState = findViewById(R.id.emptyState)
        loadingLayout = findViewById(R.id.loadingLayout)
        btnReset = findViewById(R.id.btnReset)
        btnSettings = findViewById(R.id.btnSettings)
        btnLanguage = findViewById(R.id.btnLanguage)
        headerContainer = findViewById(R.id.headerContainer)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Top padding for the header to avoid status bar overlap
            headerContainer.setPadding(
                headerContainer.paddingLeft,
                systemBars.top + 12,
                headerContainer.paddingRight,
                headerContainer.paddingBottom
            )
            // Bottom padding for the RecyclerView so items scroll cleanly above navigation bar
            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                systemBars.bottom + 24
            )
            insets
        }
    }

    private fun setupRecyclerView() {
        recyclerAdapter = SelectRecyclerAdapter(this, emptyList()).apply {
            setHasStableIds(true)
            onAppSelectedListener = { info ->
                viewModel.setHomeApp(info.id)
                finish()
                val intent = Intent(this@SelectActivity, LauncherActivity::class.java)
                startActivity(intent)
            }
        }

        with(recycler) {
            adapter = recyclerAdapter
            layoutManager = LinearLayoutManager(this@SelectActivity)
            edgeEffectFactory = RecyclerEdgeEffectFactory()
            setItemViewCacheSize(100)
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.filter(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            searchInput.text?.clear()
        }
    }

    private fun setupActions() {
        btnLanguage.setOnClickListener {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val isCurrentlyZh = currentLocales.toLanguageTags().contains("zh") ||
                    (currentLocales.isEmpty && Locale.getDefault().language == "zh")
            val targetTag = if (isCurrentlyZh) "en" else "zh-Hans"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetTag))
        }

        btnSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, getString(R.string.toast_cannot_open_settings), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReset.setOnClickListener {
            viewModel.clearHomeApp()
            Toast.makeText(this, getString(R.string.toast_reset_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.appList.observe(this) { list ->
            recyclerAdapter.appList = list
            recyclerAdapter.notifyDataSetChanged()

            appCountText.text = getString(R.string.total_apps_format, list.size)
            emptyState.visibility = if (list.isEmpty() && viewModel.isLoading.value != true) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            loadingLayout.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) {
                emptyState.visibility = View.GONE
            }
        }

        viewModel.currentHomePackage.observe(this) { currentHome ->
            btnReset.visibility = if (currentHome.isNotBlank()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateList()
    }
}