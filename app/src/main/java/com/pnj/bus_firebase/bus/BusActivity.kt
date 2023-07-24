package com.pnj.bus_firebase.bus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.R
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.pnj.bus_firebase.chat.Chat
import com.pnj.bus_firebase.chat.ChatAdapter
import com.pnj.bus_firebase.databinding.ActivityBusBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusBinding

    private lateinit var busRecyclerView: RecyclerView
    private lateinit var busArrayList: ArrayList<Bus>
    private lateinit var busAdapter: BusAdapter
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        busRecyclerView = binding.busListView
        busRecyclerView.layoutManager = LinearLayoutManager(this)
        busRecyclerView.setHasFixedSize(true)

        busArrayList = arrayListOf()
        busAdapter = BusAdapter(busArrayList)

        busRecyclerView.adapter = busAdapter

        load_data()

        binding.btnAddBus.setOnClickListener {
            val intentMain = Intent(this, AddBusActivity::class.java)
            startActivity(intentMain)
        }

        swipedDelete()

        binding.txtSearchBus.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchBus.text.toString()
                if (keyword.isNotEmpty()) {
                    search_data(keyword)
                }
                else {
                    load_data()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

    }

    private fun load_data() {
        busArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("bus").
        addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(
                value: QuerySnapshot?,
                error: FirebaseFirestoreException?
            ) {
                if(error != null){
                    Log.e("Firestore Error", error.message.toString())
                    return
                }
                for (dc: DocumentChange in value?.documentChanges!!){
                    if(dc.type == DocumentChange.Type.ADDED)
                        busArrayList.add(dc.document.toObject(Bus::class.java))
                }
                busAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun search_data(keyword : String) {
        busArrayList.clear()

        db = FirebaseFirestore.getInstance()

        val query = db.collection("bus")
            .orderBy("nama")
            .startAt(keyword)
            .get()
        query.addOnSuccessListener {
            busArrayList.clear()
            for (document in it) {
                busArrayList.add(document.toObject(Bus::class.java))
            }
        }
    }

    private fun deleteBus(bus: Bus, doc_id: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah ${bus.nama} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("Yes") {dialog, id ->
                lifecycleScope.launch {
                    db.collection("bus")
                        .document(doc_id).delete()

                    deleteFoto("img_bus/${bus.plat}_${bus.nama}.jpg")
                    Toast.makeText(
                        applicationContext,
                        bus.nama.toString() + "is deleted",
                        Toast.LENGTH_LONG
                    ).show()
                    load_data()
                }
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
                load_data()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun swipedDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    val bus = busArrayList[position]
                    val personQuery = db.collection("bus")
                        .whereEqualTo("plat",bus.plat)
                        .whereEqualTo("nama",bus.nama)
                        .whereEqualTo("jenis_bus",bus.jenis_bus)
                        .whereEqualTo("tgl_produksi",bus.tgl_produksi)
                        .whereEqualTo("service",bus.service)
                        .get()
                        .await()

                    if(personQuery.documents.isNotEmpty()) {
                        for (document in personQuery) {
                            try {
                                deleteBus(bus, document.id)
                                load_data()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        applicationContext,
                                        e.message.toString(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "Bus yang ingin dihapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }).attachToRecyclerView(busRecyclerView)
    }

    private  fun deleteFoto(file_name: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val deleteFileRef = storageRef.child(file_name)
        if (deleteFileRef != null) {
            deleteFileRef.delete().addOnCanceledListener {
                Log.e("deleted", "success")
            }.addOnFailureListener {
                Log.e("deleted", "failed")
            }
        }

    }

}