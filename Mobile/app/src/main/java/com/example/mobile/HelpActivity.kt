package com.example.mobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : AppCompatActivity() {

    private lateinit var myModelList: ArrayList<MyModel>
    private lateinit var myAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        loadCards()

        //add page change listener
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
//            override fun onPageScrolled(
//                position: Int,
//                positionOffset: Float,
//                positionOffsetPixels: Int
//            ) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onPageSelected(position: Int) {
//
//            }
//
//            override fun onPageScrollStateChanged(state: Int) {
//
//            }
//        })

    }

    private fun loadCards() {
        //init list
        myModelList = ArrayList()

        //add items to list
        myModelList.add(
            MyModel(
            1,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Mauris malesuada lacinia nunc, quis auctor ex condimentum sed. " +
                    "Integer blandit, ante eu feugiat venenatis, quam justo placerat tellus, a malesuada est est tempor nibh."
        ))
        myModelList.add(MyModel(
            2,
            "Phasellus turpis ante, placerat in leo in, imperdiet facilisis est. " +
                    "Integer vel varius purus. Sed lobortis orci ac magna bibendum auctor et tempus tempor, est quam porta sapien, nec mattis metus mi et dui."
        ))
        myModelList.add(MyModel(
            3,
            " Pellentesque nisl mi, fermentum in quam vel, porttitor condimentum risus. " +
                    "Donec mi augue, gravida id sagittis ac, sagittis vitae orci."
        ))
        myModelList.add(MyModel(
            4,
            "Donec placerat ligula eget urna sagittis porttitor. " +
                    "Maecenas eros tellus, viverra non ipsum non, congue egestas leo. " +
                    "Suspendisse at ipsum dictum, mattis ipsum ut, tempor ante. " +
                    "Quisque vitae purus non velit porta efficitur quis vel enim."
        ))
        myModelList.add(MyModel(
            5,
            "Praesent hendrerit varius justo, ac ultricies turpis posuere a. " +
                    "Donec auctor dapibus sollicitudin. " +
                    "Vivamus nulla elit, pulvinar id venenatis quis, egestas ut diam."
        ))

        //setup Adapter
        myAdapter = ViewPagerAdapter(this, myModelList)

        //set Adapter to View
        viewPager.adapter = myAdapter
        viewPager.setPadding(100, 0, 100, 0)


    }
}