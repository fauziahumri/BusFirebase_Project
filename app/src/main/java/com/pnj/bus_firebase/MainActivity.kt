package com.pnj.bus_firebase

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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.pnj.bus_firebase.auth.SettingsActivity
import com.pnj.bus_firebase.bus.BusActivity
import com.pnj.bus_firebase.chat.ChatActivity
import com.pnj.bus_firebase.databinding.ActivityMainBinding
import com.pnj.bus_firebase.penumpang.AddPenumpangActivity
import com.pnj.bus_firebase.penumpang.Penumpang
import com.pnj.bus_firebase.penumpang.PenumpangAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var penumpangRecyclerView: RecyclerView
    private lateinit var penumpangArrayList: ArrayList<Penumpang>
    private lateinit var penumpangAdapter: PenumpangAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        penumpangRecyclerView = binding.penumpangListView
        penumpangRecyclerView.layoutManager = LinearLayoutManager(this)
        penumpangRecyclerView.setHasFixedSize(true)

        penumpangArrayList = arrayListOf()
        penumpangAdapter = PenumpangAdapter(penumpangArrayList)

        penumpangRecyclerView.adapter = penumpangAdapter

        load_data()

        binding.btnAddPenumpang.setOnClickListener {
            val intentMain = Intent(this, AddPenumpangActivity::class.java)
            startActivity(intentMain)
        }

        swipedDelete()

        binding.txtSearchPenumpang.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchPenumpang.text.toString()
                if (keyword.isNotEmpty()) {
                    search_data(keyword)
                }
                else {
                    load_data()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.nav_bottom_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_bus -> {
                    val intent = Intent(this, BusActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_chat -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun load_data() {
        penumpangArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("penumpang").
                addSnapshotListener(object : EventListener<QuerySnapshot> {
                    override fun onEvent(
                        value: QuerySnapshot?,
                        error: FirebaseFirestoreException?
                    ) {
                        if (error != null) {
                            Log.e("Firestore Error", error.message.toString())
                            return
                        }
                        for (dc: DocumentChange in value?.documentChanges!!) {
                            if (dc.type == DocumentChange.Type.ADDED)
                                penumpangArrayList.add(dc.document.toObject(Penumpang::class.java))
                        }
                        penumpangAdapter.notifyDataSetChanged()
                    }
                })
    }

    private fun search_data(keyword : String) {
        penumpangArrayList.clear()
        db = FirebaseFirestore.getInstance()

        val query = db.collection("penumpang")
            .orderBy("nama")
            .startAt(keyword)
            .get()
        query.addOnSuccessListener {
            penumpangArrayList.clear()
            for (document in it) {
                penumpangArrayList.add(document.toObject(Penumpang::class.java))
            }
        }
    }

    private fun deletePenumpang(penumpang: Penumpang, doc_id: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah ${penumpang.nama} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("Yes") {dialog, id ->
                lifecycleScope.launch {
                    db.collection("penumpang")
                        .document(doc_id).delete()

                    deleteFoto("img_penumpang/${penumpang.nik}_${penumpang.nama}.jpg")
                    Toast.makeText(
                        applicationContext,
                        penumpang.nama.toString() + "is deleted",
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
                    val penumpang = penumpangArrayList[position]
                    val personQuery = db.collection("penumpang")
                        .whereEqualTo("plat",penumpang.nik)
                        .whereEqualTo("nama",penumpang.nama)
                        .whereEqualTo("jenis_kelamin",penumpang.jenis_kelamin)
                        .whereEqualTo("tgl_berangkat",penumpang.tgl_berangkat)
                        .whereEqualTo("tujuan",penumpang.tujuan)
                        .get()
                        .await()

                    if(personQuery.documents.isNotEmpty()) {
                        for (document in personQuery) {
                            try {
                                deletePenumpang(penumpang, document.id)
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
                                "Penumpang yang ingin dihapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }).attachToRecyclerView(penumpangRecyclerView)
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