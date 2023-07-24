package com.pnj.bus_firebase.bus

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.pnj.bus_firebase.R
import java.io.File

class BusAdapter(private val busList: ArrayList<Bus>) :
    RecyclerView.Adapter<BusAdapter.BusViewHolder>() {

    private lateinit var activity: AppCompatActivity

    class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val plat: TextView = itemView.findViewById(R.id.TVLPlat)
            val nama: TextView = itemView.findViewById(R.id.TVLNama)
            val jenis_bus: TextView = itemView.findViewById(R.id.TVLJenisBus)
            val img_bus : ImageView = itemView.findViewById(R.id.IMLGambarBus)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.bus_list_layout, parent, false)
        return BusViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        val bus: Bus = busList[position]
        holder.plat.text = bus.plat
        holder.nama.text = bus.nama
        holder.jenis_bus.text = bus.jenis_bus

//        holder.itemView.setOnClickListener{
//            activity = it.context as AppCompatActivity
//            activity.startActivity(Intent(activity, EditBusActivity::class.java).apply {
//                putExtra("plat", bus.plat.toString())
//                putExtra("nama", bus.nama.toString())
//                putExtra("tgl_produksi", bus.tgl_produksi.toString())
//                putExtra("jenis_bus", bus.jenis_bus.toString())
//                putExtra("service", bus.service.toString())
//            })
//        }
        val  storageRef = FirebaseStorage.getInstance().reference.child("img_pasien/${bus.plat}_${bus.nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.img_bus.setImageBitmap(bitmap)
        }.addOnFailureListener{
            Log.e("foto ?", "gagal")
        }

    }

    override fun getItemCount(): Int {
        return busList.size
    }
}