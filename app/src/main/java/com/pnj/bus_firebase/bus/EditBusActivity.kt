package com.pnj.bus_firebase.bus

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.pnj.bus_firebase.MainActivity
import com.pnj.bus_firebase.databinding.ActivityEditBusBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class EditBusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBusBinding
    private val db = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_bus) = setDefaultValue()

        binding.TxtEditTglProduksi.setOnClickListener {
            val dpd = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    binding.TxtEditTglProduksi.setText(
                        "" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth
                    )
                }, year.toString().toInt(), month.toString().toInt(), day.toString().toInt()
            )
            dpd.show()
        }

        binding.BtnEditBus.setOnClickListener {
            val new_data_bus = newBus()
            updateBus(curr_bus as Bus, new_data_bus)

            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        showFoto()

        binding.BtnImgBus.setOnClickListener {
            openCamera()
        }
    }

    fun setDefaultValue(): Array<Any> {
        val intent = intent
        val plat = intent.getStringExtra("plat").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tgl_produksi = intent.getStringExtra("tgl_produksi").toString()
        val jenis_bus = intent.getStringExtra("jenis_bus").toString()
        val service = intent.getStringExtra("service").toString()

        binding.TxtEditPlat.setText(plat)
        binding.TxtEditNama.setText(nama)
        binding.TxtEditTglProduksi.setText(tgl_produksi)

        val tgl_split = intent.getStringExtra("tgl_produksi")
            .toString().split("-").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()
        if (jenis_bus == "Ultra High Dacker") {
            binding.RdnEditJBUHD.isChecked = true
        } else if (jenis_bus == "Double Decker") {
            binding.RdnEditJBDD.isChecked = true
        }

        val curr_bus = Bus(plat, nama, tgl_produksi, jenis_bus, service)
        return arrayOf(year, month, day, curr_bus)
    }

    fun newBus(): Map<String, Any> {
        var plat: String = binding.TxtEditPlat.text.toString()
        var nama: String = binding.TxtEditNama.text.toString()
        var tgl_produksi: String = binding.TxtEditTglProduksi.text.toString()

        var jb: String = ""
        if (binding.RdnEditJBUHD.isChecked) {
            jb = "Ultra High Decker"
        } else if (binding.RdnEditJBDD.isChecked) {
            jb = "Double Decker"
        }
        var service = ArrayList<String>()
        if (binding.ChkEditSelimut.isChecked) {
            service.add("selimut")
        }
        if (binding.ChkEditBantal.isChecked) {
            service.add("bantal")
        }
        if (binding.ChkEditCamilan.isChecked) {
            service.add("camilan")
        }
        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${plat}_${nama}")
        }

        val service_string = service.joinToString("|")
        val bus = mutableMapOf<String, Any>()
        bus["plat"] = plat
        bus["nama"] = nama
        bus["tgl_produksi"] = tgl_produksi
        bus["jenis_bus"] = jb
        bus["service"] = service

        return bus
    }

    private fun updateBus(bus: Bus, newBusMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personaQuery = db.collection("bus")
                .whereEqualTo("plat", bus.plat)
                .whereEqualTo("nama", bus.nama)
                .whereEqualTo("jenis_bus", bus.jenis_bus)
                .whereEqualTo("tgl_produksi", bus.tgl_produksi)
                .whereEqualTo("service", bus.service)
                .get()
                .await()
            if (personaQuery.documents.isNotEmpty()) {
                for (document in personaQuery) {
                    try {
                        db.collection("bus").document(document.id).set(
                            newBusMap,
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EditBusActivity,
                                e.message, Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditBusActivity,
                        "No persons matched the query.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    fun showFoto() {
        val intent = intent
        val plat = intent.getStringExtra("plat").toString()
        val nama = intent.getStringExtra("nama").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_bus/${plat}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgBus.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?", "gagal")
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also{
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgBus.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_bus/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener{
                if(it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let {Uri ->
                            imgUri = Uri
                            binding.BtnImgBus.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
}