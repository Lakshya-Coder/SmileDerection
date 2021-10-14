package com.lakshyagupta7089.smiledetection

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lakshyagupta7089.smiledetection.databinding.ItemFaceDetectionBinding

class FaceDetectionAdapter(
    private val context: Context,
    private val faceDetectionModelList: ArrayList<FaceDetectionModel>
): RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFaceDetectionBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val faceDetectionModel = faceDetectionModelList[position]

        holder.binding.itemFaceDetectionTextView1.text = faceDetectionModel.id.toString()
        holder.binding.itemFaceDetectionTextView2.text = faceDetectionModel.text
    }

    override fun getItemCount(): Int = faceDetectionModelList.size

    inner class ViewHolder(val binding: ItemFaceDetectionBinding)
        : RecyclerView.ViewHolder(binding.root) {
    }
}