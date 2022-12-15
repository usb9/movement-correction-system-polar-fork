package com.example.mobile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.synthetic.main.card_item.view.*

class ViewPagerAdapter(private val  context: Context, private val myModelArrayList: ArrayList<MyModel>)
    : RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> (){

    inner class ViewPagerViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false)
        return ViewPagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {

        //get data
        val model = myModelArrayList[position]
        val title = model.title
        val description = model.description

        //set data to view holder
        holder.itemView.title_tv.text = title.toString()
        holder.itemView.description_tv.text = description

    }

    override fun getItemCount(): Int {
        return myModelArrayList.size     //return list of items
    }


    //    override fun getCount(): Int {
//        return myModelArrayList.size     //return list of items
//    }
//
//    override fun isViewFromObject(view: View, `object`: Any): Boolean {
//        return view == `object`
//    }
//
//    override fun instantiateItem(container: ViewGroup, position: Int): Any {
//        val view = LayoutInflater.from(context).inflate(R.layout.card_item, container, false)

//        //get data
//        val model = myModelArrayList[position]
//        val title = model.title
//        val description = model.description
//
//        //set data to ui views
//        view.title_tv.text = title.toString()
//        view.description_tv.text = description
//
//        //add view to container
//        container.addView(view, position)
//
//        return view
//    }
//
//    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
//        container.removeView(`object` as View) {
//        }
//    }
}