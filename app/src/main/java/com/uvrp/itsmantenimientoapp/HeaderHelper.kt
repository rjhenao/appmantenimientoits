package com.uvrp.itsmantenimientoapp.helpers

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import com.uvrp.itsmantenimientoapp.BitacorasActivity
import com.uvrp.itsmantenimientoapp.HomeActivity
import com.uvrp.itsmantenimientoapp.MainActivity
import com.uvrp.itsmantenimientoapp.Nivel1Activity
import com.uvrp.itsmantenimientoapp.R
import com.uvrp.itsmantenimientoapp.SeleccionEquipoActivity
import com.uvrp.itsmantenimientoapp.iniciarPreoperacional

object HeaderHelper {

    private var toggle: ActionBarDrawerToggle? = null

    fun setupHeader(
        activity: AppCompatActivity,
        drawerLayout: DrawerLayout,
        navView: NavigationView
    ) {
        val toolbar: Toolbar = activity.findViewById(R.id.toolbar_home)
            ?: run {
                Toast.makeText(activity, "No se encontró el Toolbar (id: toolbar_home)", Toast.LENGTH_LONG).show()
                return
            }

        activity.setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).also {
            drawerLayout.addDrawerListener(it)
            it.syncState()
        }

        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_toolbar)

        val sharedPreferences = activity.getSharedPreferences("Sesion", AppCompatActivity.MODE_PRIVATE)
        val idRol = sharedPreferences.getInt("idRol", -1)

        // --- INICIO DE LA MEJORA PRINCIPAL ---
        // 1. Ocultar ítems del menú si el usuario no tiene el rol adecuado.
        //    Esto se hace ANTES de que el usuario pueda hacer clic.
        val menu = navView.menu
        val tienePermisosITS = (idRol == 1 || idRol == 2)
        val tienePermisosMantenimiento = (idRol == 1 || idRol == 5 || idRol == 6)


        menu.findItem(R.id.nav_its).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_correctivo).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_bitacoras).isVisible = tienePermisosMantenimiento

        // --- FIN DE LA MEJORA PRINCIPAL ---

        // Listener del menú del toolbar (Cerrar Sesión)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_cerrar_sesion -> {
                    logout(activity) // Usamos la función centralizada
                    true
                }
                else -> false
            }
        }

        // Listener del menú lateral (NavigationView)
        navView.setNavigationItemSelectedListener { item ->
            // 2. Se simplifica el listener: ya no necesitamos comprobar el rol aquí.
            //    Si el ítem es visible, es porque el usuario tiene permiso.
            when (item.itemId) {
                R.id.nav_its -> navigateTo(activity, Nivel1Activity::class.java)
                R.id.nav_home -> navigateTo(activity, HomeActivity::class.java)
                R.id.nav_correctivo -> navigateTo(activity, SeleccionEquipoActivity::class.java)
                R.id.nav_preoperacional -> navigateTo(activity, iniciarPreoperacional::class.java)
                R.id.nav_bitacoras -> navigateTo(activity, BitacorasActivity::class.java)
                R.id.nav_cerrarsesion -> logout(activity) // Usamos la función centralizada
            }
            // Cierra el menú lateral después de la selección
            drawerLayout.closeDrawers()
            true
        }
    }

    fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return toggle?.onOptionsItemSelected(item) ?: false
    }

    // 3. Función centralizada para navegar, evitando código repetido.
    private fun navigateTo(activity: AppCompatActivity, destination: Class<*>) {
        activity.startActivity(Intent(activity, destination))
    }

    // 4. Función centralizada para cerrar sesión, evitando código duplicado.
    private fun logout(activity: AppCompatActivity) {
        activity.getSharedPreferences("Sesion", AppCompatActivity.MODE_PRIVATE).edit {
            clear()
        }
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}