package com.uvrp.itsmantenimientoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uvrp.itsmantenimientoapp.adapters.UsuarioSeleccionAdapter
import com.uvrp.itsmantenimientoapp.models.Usuario

class SeleccionarUsuarioDialogFragment : DialogFragment() {

    // --- Componentes de la UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnConfirmar: Button
    private lateinit var btnCancelar: Button

    // --- Lógica y Datos ---
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: UsuarioSeleccionAdapter
    private var fullUserList: List<Usuario> = listOf()
    private val selectedUserIds = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout que diseñamos para el diálogo
        return inflater.inflate(R.layout.dialog_seleccionar_usuario, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar los componentes de la UI usando findViewById
        recyclerView = view.findViewById(R.id.recycler_view_usuarios_seleccion)
        searchView = view.findViewById(R.id.search_view_usuarios)
        btnConfirmar = view.findViewById(R.id.button_confirmar)
        btnCancelar = view.findViewById(R.id.button_cancelar)

        // 2. Inicializar la base de datos
        dbHelper = DatabaseHelper(requireContext())

        // 3. Obtener la lista de IDs a excluir que viene desde la Activity
        val idsExcluidos = arguments?.getIntegerArrayList("ids_excluidos") ?: listOf<Int>()

        // 4. Cargar la lista completa de usuarios y filtrar los que ya están en la pantalla principal
        fullUserList = dbHelper.getAllUsers().filter { it.id !in idsExcluidos }

        // 5. Configurar el RecyclerView
        adapter = UsuarioSeleccionAdapter(fullUserList, selectedUserIds)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // 6. Configurar los listeners para los botones y el buscador
        setupListeners()
    }

    private fun setupListeners() {
        // Acción para el botón de cancelar
        btnCancelar.setOnClickListener {
            dismiss() // Simplemente cierra el diálogo
        }

        // Acción para el botón de confirmar
        btnConfirmar.setOnClickListener {
            onConfirmarClick()
        }

        // Acción para el buscador (SearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // No necesitamos hacer nada cuando el usuario presiona "Enter"
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            // Este se activa cada vez que el texto en el buscador cambia
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim() ?: ""
                // Filtramos la lista completa de usuarios
                val filteredList = fullUserList.filter {
                    it.nombre.contains(query, ignoreCase = true)
                }
                // Le decimos al adapter que actualice la vista con la lista filtrada
                adapter.updateList(filteredList)
                return true
            }
        })
    }

    private fun onConfirmarClick() {
        // Preparamos el paquete de datos ("bundle") para enviar de vuelta a la Activity
        val resultado = Bundle().apply {
            putIntegerArrayList("ids_seleccionados", ArrayList(selectedUserIds))
        }
        // Usamos el FragmentManager para enviar el resultado
        parentFragmentManager.setFragmentResult("seleccion_usuarios_request", resultado)
        // Cerramos el diálogo
        dismiss()
    }

    companion object {
        // Este es el método que se usa para crear el diálogo de forma segura,
        // asegurando que los argumentos (los IDs a excluir) se pasen correctamente.
        fun newInstance(idsExcluidos: ArrayList<Int>): SeleccionarUsuarioDialogFragment {
            val fragment = SeleccionarUsuarioDialogFragment()
            val args = Bundle().apply {
                putIntegerArrayList("ids_excluidos", idsExcluidos)
            }
            fragment.arguments = args
            return fragment
        }
    }
}