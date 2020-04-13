package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.fragment.ProfileEditFragment
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.model.ProfileMetadata
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_profile_edit.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProfileEditActivity : BaseActivity() {
    private var modified = false
    private var processing = false
        set(value) {
            field = value

            if (value) {
                saving.visibility = View.VISIBLE
                save.visibility = View.INVISIBLE
            } else {
                saving.visibility = View.INVISIBLE
                save.visibility = View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)
        setSupportActionBar(toolbar)

        toolbar.setTitle(R.string.loading)

        launch {
            val id = intent.getLongExtra("id", -1)

            val metadata = withProfile {
                queryById(id)
            } ?: return@launch finish()

            if (metadata.name.isBlank())
                toolbar.setTitle(R.string.new_profile)
            else
                toolbar.setTitle(R.string.edit_profile)

            val fragment = ProfileEditFragment(
                metadata.name, metadata.uri, metadata.interval,
                metadata.type, metadata.source
            )

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit()

            save.setOnClickListener {
                val name = fragment.name
                val uri = fragment.uri
                val interval = fragment.interval

                if (name.isBlank()) {
                    Snackbar.make(rootView, R.string.empty_name, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val newMetadata = metadata.copy(
                    name = name,
                    uri = uri,
                    interval = interval
                )

                processing = true

                commit(newMetadata)
            }
        }
    }

    override fun onBackPressed() {
        if (!modified)
            return super.onBackPressed()

        if (processing) {
            Snackbar.make(rootView, R.string.processing, Snackbar.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.exit_without_save)
            .setMessage(R.string.exit_without_save_warning)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        runBlocking {
            withProfile {
                cancel(intent.getLongExtra("id", -1))
            }
        }

        super.onDestroy()
    }

    private fun commit(metadata: ProfileMetadata) {
        launch {
            withProfile {
                updateMetadata(metadata.id, metadata)
                commitAsync(metadata.id)
            }
        }
    }
}