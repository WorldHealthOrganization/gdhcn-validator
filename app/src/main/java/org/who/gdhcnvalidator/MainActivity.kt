package org.who.gdhcnvalidator

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import org.who.gdhcnvalidator.databinding.ActivityMainBinding

class MainActivity : AuthActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var mMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onAccountState(isReady: Boolean, isAuthorized: Boolean) {
        if (!this::mMenu.isInitialized) return
        mMenu.findItem(R.id.action_sign_in).isEnabled = isReady
        mMenu.findItem(R.id.action_logout).isEnabled = isReady
        mMenu.findItem(R.id.action_sign_in).isVisible = !isAuthorized
        mMenu.findItem(R.id.action_logout).isVisible = isAuthorized
    }

    override fun onNewUserInfo(userInfo: User) {
        Toast.makeText(this, "Welcome " + userInfo.name, Toast.LENGTH_SHORT).show()
    }

    override fun backgroundInit() {
        super.backgroundInit()
        // kicks of the loading of Fhir Engine, Operator and Trust Registry
        FhirApplication.initInMemory(applicationContext)
        FhirApplication.trustRegistry(applicationContext)
        FhirApplication.fhirContext(applicationContext)
        TODO("FHIR Engine currently leads to app crash on startup")
//        FhirApplication.fhirEngine(applicationContext)
//        FhirApplication.fhirOperator(applicationContext)
//        FhirApplication.subscribedIGs(applicationContext)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        updateAccountState()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_sign_in -> { requestAuthorization(); return true; }
            R.id.action_logout -> { requestSignOff(); return true; }
            R.id.action_change_trust_lists -> { showTrustListDialog(); return true; }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun showTrustListDialog() {
        val registries = FhirApplication.trustRegistry(this.applicationContext).scopeNames()

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_active_trust_lists)
        builder.setMultiChoiceItems(
            registries.map { it.entity.name }.toTypedArray(),
            registries.map { it.active }.toBooleanArray()
        ) { dialog, which, isChecked ->
            registries[which].active = isChecked
        }
        builder.setPositiveButton(
            R.string.ok,
        ) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        builder.show()
    }
}