
package com.example.mobile


import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_statistic.*


class StatisticActivity : AppCompatActivity() {

       public val correctLable ="Correct"
       public val incorrectLable ="Incorrect"

     val dataPieChart: ArrayList<Float> = ArrayList()
    val dataBarChart: ArrayList<ArrayList<Float>> = ArrayList()
    val dataLineChart: ArrayList<ArrayList<Float>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistic)

        dataPieChart.add(150f)
        dataPieChart.add(50f)
        setUpPieChartData(dataPieChart)

        var barChart_totalPunches :ArrayList<Float>  = ArrayList<Float>(listOf(30f,2f,4f,6f,8f,10f,22f))
        var barChart_correctTimes :ArrayList<Float>  = ArrayList<Float>(listOf(10f,2f,3f,32f,22f,30f,12f))
        var barChart_incorrectTime :ArrayList<Float>  = ArrayList<Float>(listOf(20f,22f,23f,22f,32f,20f,32f))
        dataBarChart.add(barChart_totalPunches);
        dataBarChart.add(barChart_correctTimes)
        dataBarChart.add(barChart_incorrectTime)
        setupBarChartData(dataBarChart)

        var lineChart_incorrectTimes :ArrayList<Float>  = ArrayList<Float>(listOf(30f,2f,4f,6f,8f,10f,22f))
        var lineChart_correctTimes :ArrayList<Float>  = ArrayList<Float>(listOf(10f,2f,3f,32f,22f,30f,12f))

        dataLineChart.add(lineChart_correctTimes)
        dataLineChart.add(lineChart_incorrectTimes)
        setupLineChartData(dataLineChart)


    }
    private fun setUpPieChartData(dataLineChart :  ArrayList<Float>) {
        //Set data value here
        var correctTimes = dataLineChart[0]
        var incorrectTimes = dataLineChart[1]
        //Create dataset for graph here
        val yVals = ArrayList<PieEntry>()
        yVals.add(PieEntry(correctTimes,correctLable))
        yVals.add(PieEntry(incorrectTimes,incorrectLable))
        //Set data to data pie chart
        val dataSet = PieDataSet(yVals,"")
        //Format float to %
        dataSet.valueFormatter = MyValueFormatter()
        //Set color for element
        val colors = java.util.ArrayList<Int>()
        colors.add(ContextCompat.getColor(this, R.color.customer1) )
        colors.add(ContextCompat.getColor(this, R.color.customer2) )


        dataSet.colors = colors
        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true);
        pieChart.data.setValueTextSize(35F)
        pieChart.setEntryLabelColor(Color.BLACK)


        pieChart.isRotationEnabled=false
        pieChart.isDrawHoleEnabled = false
        pieChart.legend.isEnabled=false
        pieChart.description.isEnabled = false
        pieChart.invalidate();
    }

    private fun setupLineChartData(dataLineChart : ArrayList< ArrayList<Float>>) {

        var correctTimes = dataLineChart[0]
        var incorrectTimes = dataLineChart[1]

        val correctLine: ArrayList<Entry> = ArrayList()
        val incorrectLine: ArrayList<Entry> = ArrayList()

        for (i in correctTimes.indices) {
            correctLine.add(Entry(i.toFloat(), correctTimes[i], i.toString()))
            incorrectLine.add(Entry(i.toFloat(), incorrectTimes[i], i.toString()))
        }

        val set1: LineDataSet = LineDataSet(correctLine, correctLable)


        val set2: LineDataSet = LineDataSet(incorrectLine, incorrectLable)

        // set1.fillAlpha = 110
        // set1.setFillColor(Color.RED);

        // set the line to be drawn like this "- - - - - -"
        // set1.enableDashedLine(5f, 5f, 0f);
        // set1.enableDashedHighlightLine(10f, 5f, 0f);
        set1.color = ContextCompat.getColor(this, R.color.customer1)
        set1.setCircleColor(ContextCompat.getColor(this, R.color.customer1))
        set1.lineWidth = 1f
        set1.circleRadius = 3f
        set1.setDrawCircleHole(false)
        set1.valueTextSize = 0f
        set1.setDrawFilled(false)

        set2.color = Color.BLACK
        set2.setCircleColor(ContextCompat.getColor(this, R.color.customer2))
        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(set1)
        dataSets.add(set2)
        val data = LineData(dataSets)

        // set data
        lineChart.setData(data)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.data.setValueTextSize(10F)
        lineChart.legend.horizontalAlignment=Legend.LegendHorizontalAlignment.CENTER
        lineChart.setTouchEnabled(false)

        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.invalidate()
    }

    private fun setupBarChartData(dataBarChart : ArrayList< ArrayList<Float>>) {
        // create BarEntry for Bar Group

        var totalPunches = dataBarChart[0]
        var totalCorrectPunches =dataBarChart[1]
        var totalIncorrectPunches = dataBarChart[2]


        val correctBar:ArrayList<BarEntry> = ArrayList()
        val incorrectBar: ArrayList<BarEntry> = ArrayList()
        val totalBar: ArrayList<BarEntry> = ArrayList()


        for (i in totalPunches.indices) {
            totalBar.add(BarEntry(i.toFloat(), totalPunches[i]))
            correctBar.add(BarEntry(i.toFloat(), totalCorrectPunches[i]))
            incorrectBar.add(BarEntry(i.toFloat(), totalIncorrectPunches[i]))

        }

        val labels = ArrayList<String>()
        labels.add("Monday")
        labels.add("Tuesday")
        labels.add("Wednesday")
        labels.add("Thursday")
        labels.add("Friday")
        labels.add("Saturday")
        labels.add("Sunday")
//
//        val bargroup1 = ArrayList<BarEntry>()
//        bargroup1.add(BarEntry(0f, 30f))
//        bargroup1.add(BarEntry(1f, 2f))
//        bargroup1.add(BarEntry(2f, 4f))
//        bargroup1.add(BarEntry(3f, 6f))
//
//        val bargroup2 = ArrayList<BarEntry>()
//        bargroup2.add(BarEntry(0f, 20f))
//        bargroup2.add(BarEntry(1f, 22f))
//        bargroup2.add(BarEntry(2f, 24f))
//        bargroup2.add(BarEntry(3f, 26f))



        // creating dataset for Bar Group
        val barDataSet1 = BarDataSet(totalBar, "Total punches")
        val barDataSet2 = BarDataSet(incorrectBar, incorrectLable)
        val barDataSet3 = BarDataSet(correctBar, correctLable)


        barDataSet1.color = ContextCompat.getColor(this, R.color.purple_700)
        barDataSet2.color = ContextCompat.getColor(this, R.color.customer1)
        barDataSet3.color = ContextCompat.getColor(this, R.color.customer2)

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(barDataSet1)
        dataSets.add(barDataSet2)
        dataSets.add(barDataSet3)
        val data = BarData(dataSets)
        val groupSpace =0.22f
        val barSpace =  0f
        val barWidth =  0.26f



        barChart.data=data
        data.barWidth = barWidth;
        barChart.groupBars(0f,groupSpace,barSpace)

        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        val xAxis = barChart.xAxis

        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.position = XAxis.XAxisPosition.BOTTOM;
        xAxis.labelCount = labels.size;
        xAxis.valueFormatter =
            IAxisValueFormatter { value, axis ->
                if (value >= 0) {
                    return@IAxisValueFormatter if (value < labels.size) labels[value.toInt()] else ""
                }
                return@IAxisValueFormatter ""
            }
        xAxis.axisMinimum = 0f;
        xAxis.axisMaximum = correctBar.size.toFloat() ;
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.legend.isEnabled = true
        barChart.legend.horizontalAlignment=Legend.LegendHorizontalAlignment.CENTER
        barChart.setPinchZoom(false)
        barChart.data.setDrawValues(true)
        barChart.setFitBars(true);
        barChart.setTouchEnabled(false)
        barChart.invalidate();
    }

}