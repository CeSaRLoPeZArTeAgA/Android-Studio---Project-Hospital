package com.example.mykhospital

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HospitalAdapter(
    private val lista: List<Hospital>,
    private val onItemClick: (Hospital) -> Unit,
    private val onItemLongClick: (Hospital) -> Unit
) : RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    inner class HospitalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtDireccion: TextView = itemView.findViewById(R.id.txtDireccion)
        val txtEspecialidades: TextView = itemView.findViewById(R.id.txtEspecialidades)
        val imgHospital: ImageView = itemView.findViewById(R.id.imgHospital)

        fun bind(h: Hospital) {
            txtNombre.text = h.nombre
            txtDireccion.text = h.direccion
            txtEspecialidades.text =
                if (h.especialidades.isEmpty()) "-" else h.especialidades.joinToString(", ")

            // muestra imagen base64 si existe
            if (h.imgBase64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(h.imgBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imgHospital.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imgHospital.setImageResource(android.R.drawable.ic_menu_mylocation)
                }
            } else {
                imgHospital.setImageResource(android.R.drawable.ic_menu_mylocation)
            }

            itemView.setOnClickListener {
                onItemClick(h)
            }
            itemView.setOnLongClickListener {
                onItemLongClick(h)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size
}
