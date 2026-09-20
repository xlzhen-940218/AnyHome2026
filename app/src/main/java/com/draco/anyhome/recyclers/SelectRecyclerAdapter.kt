package com.draco.anyhome.recyclers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.draco.anyhome.R
import com.draco.anyhome.models.AppInfo
import com.draco.anyhome.views.LauncherActivity

class SelectRecyclerAdapter(
    private val context: Context,
    var appList: List<AppInfo>
) : RecyclerView.Adapter<SelectRecyclerAdapter.ViewHolder>() {
    private lateinit var sharedPrefs: SharedPreferences
    private val packageManager: PackageManager = context.packageManager
    var onAppSelectedListener: ((AppInfo) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val name: TextView = itemView.findViewById(R.id.name)
        val pkgName: TextView = itemView.findViewById(R.id.pkgName)
        val currentBadge: View = itemView.findViewById(R.id.currentBadge)

        val translationY: SpringAnimation = SpringAnimation(itemView, SpringAnimation.TRANSLATION_Y).apply {
            spring = SpringForce()
                .setFinalPosition(0f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = appList[position]

        holder.name.text = info.label
        holder.pkgName.text = info.id
        holder.currentBadge.visibility = if (info.isCurrentHome) View.VISIBLE else View.GONE

        // Load application icon asynchronously
        try {
            val iconDrawable = packageManager.getApplicationIcon(info.id)
            Glide.with(holder.img.context)
                .load(iconDrawable)
                .fitCenter()
                .into(holder.img)
        } catch (e: Exception) {
            holder.img.setImageResource(R.mipmap.ic_launcher)
        }

        holder.itemView.setOnClickListener {
            if (onAppSelectedListener != null) {
                onAppSelectedListener?.invoke(info)
            } else {
                with(sharedPrefs.edit()) {
                    putString("home_app", info.id)
                    apply()
                }

                if (context is AppCompatActivity) {
                    context.finish()
                }
                val intent = Intent(context, LauncherActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    override fun getItemId(position: Int): Long {
        return appList[position].id.hashCode().toLong()
    }
}