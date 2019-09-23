package nz.co.olliechick.hivo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_onboarding.*
import nz.co.olliechick.hivo.util.Preferences.Companion.setOnboardingIsComplete
import nz.co.olliechick.hivo.util.Ui.Companion.toast


class OnboardingActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_onboarding)

        val adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> OnboardingFragment1()
                    1 -> OnboardingFragment2()
                    else -> OnboardingFragment3()
                }
            }

            override fun getCount() = 3
        }

        pager.adapter = adapter
        indicator.setViewPager(pager)

        skip.setOnClickListener { finishOnboarding() }

        toast("oncreate")

        //next.setOnClickListener { toast("hi there") }

        next.setOnClickListener {
            toast("next clicked")
            if (pager.currentItem == 2) { // The last screen
                finishOnboarding()
            } else {
                pager.setCurrentItem(
                    pager.currentItem + 1,
                    true
                )
            }
        }


        indicator.setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (position == 2) {
                    skip.visibility = View.GONE
                    next.text = "Done"
                } else {
                    skip.visibility = View.VISIBLE
                    next.text = "Next"
                }
            }
        })
    }

    private fun finishOnboarding() {
        setOnboardingIsComplete(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish() //close onboarding activity
    }
}