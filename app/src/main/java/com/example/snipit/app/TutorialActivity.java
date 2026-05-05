package com.example.snipit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.snipit.app.auth.LoginActivity;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private Button btnNext;
    private TutorialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        if (pref.getBoolean("tutorial_done", false)) {
            // If done, skip to Login immediately
            startActivity(new Intent(this, com.example.snipit.app.auth.LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_tutorial);

        viewPager = findViewById(R.id.viewPagerTutorial);
        layoutDots = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);

        adapter = new TutorialAdapter();
        viewPager.setAdapter(adapter);
        setupIndicators(adapter.getItemCount());
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentIndicator(position);
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishTutorial();
            }
        });
    }

    private void setupIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(params);
            layoutDots.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutDots.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_active));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive));
            }
        }
    }

    private void finishTutorial() {
        // Save that the user has seen the tutorial
        SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        pref.edit().putBoolean("tutorial_done", true).apply();

        // Redirect to Login/Signup screen
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close tutorial so user can't "back" into it
    }

    private class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.ViewHolder> {
        private final int[] images = {
                R.drawable.ic_vault_large,
                R.drawable.ic_scan_large,
                R.drawable.ic_ai_large
        };
        private final String[] titles = {"Secure Vault", "Instant Scanning", "AI Insights"};
        private final String[] desc = {
                "Organize and protect your code snippets with enterprise-grade encryption.",
                "Capture text and data from any physical source using high-precision OCR.",
                "Leverage advanced machine learning to analyze and optimize your snippets."
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutorial_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.img.setImageResource(images[position]);
            holder.title.setText(titles[position]);
            holder.description.setText(desc[position]);
        }

        @Override
        public int getItemCount() { return 3; }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView title, description;
            ViewHolder(View v) {
                super(v);
                img = v.findViewById(R.id.tutorialImage);
                title = v.findViewById(R.id.tutorialTitle);
                description = v.findViewById(R.id.tutorialDescription);
            }
        }
    }
}