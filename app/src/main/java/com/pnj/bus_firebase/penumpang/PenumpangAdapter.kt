package com.pnj.bus_firebase.penumpang

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

class PenumpangAdapter(private val penumpangList: ArrayList<Penumpang>) :
    RecyclerView.Adapter<PenumpangAdapter.PenumpangViewHolder> () {

        private lateinit var activity: AppCompatActivity

    class PenumpangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nik: TextView = itemView.findViewById(R.id.TVLNik)
        val nama: TextView = itemView.findViewById(R.id.TVLNama)
        val tujuan: TextView = itemView.findViewById((R.id.TVLKotaTujuan))
        val img_penumpang : ImageView = itemView.findViewById(R.id.IMLGambarPenumpang)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenumpangViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.penumpang_list_layout, parent, false)
        return PenumpangViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PenumpangViewHolder, position: Int) {
        val penumpang: Penumpang = penumpangList[position]
        holder.nik.text = penumpang.nik
        holder.nama.text = penumpang.nama
        holder.tujuan.text = penumpang.tujuan

        holder.itemView.setOnClickListener{
            activity = it.context as AppCompatActivity
            activity.startActivity(Intent(activity, EditPenumpangActivity::class.java).apply {
                putExtra("nik", penumpang.nik.toString())
                putExtra("nama", penumpang.nama.toString())
                putExtra("jenis_kelamin", penumpang.jenis_kelamin.toString())
                putExtra("tgl_berangkat", penumpang.tgl_berangkat.toString())
                putExtra("Tujuan", penumpang.tujuan.toString())
            })
        }
        val  storageRef = FirebaseStorage.getInstance().reference.child("img_penumpamg/${penumpang.nik}_${penumpang.nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.img_penumpang.setImageBitmap(bitmap)
        }.addOnFailureListener{
            Log.e("foto ?", "gagal")
        }
    }

    override fun getItemCount(): Int {
        return penumpangList.size
    }

}