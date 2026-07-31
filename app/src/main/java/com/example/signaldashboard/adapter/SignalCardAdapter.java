package com.example.signaldashboard.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signaldashboard.R;
import com.example.signaldashboard.SignalManager;
import com.example.signaldashboard.model.Signal;
import com.example.signaldashboard.util.TimeframeUtil;
import com.example.signaldashboard.view.CountdownCircleView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SignalCardAdapter extends RecyclerView.Adapter<SignalCardAdapter.CardViewHolder> {

    public interface OnCardDismissListener {
        void onDismiss(String symbol);
    }

    private static final Comparator<Signal> BY_RANK_ASC = new Comparator<Signal>() {
        @Override
        public int compare(Signal a, Signal b) {
            return Integer.compare(TimeframeUtil.rank(a.timeframe), TimeframeUtil.rank(b.timeframe));
        }
    };

    private static final Comparator<Signal> BY_RANK_DESC = new Comparator<Signal>() {
        @Override
        public int compare(Signal a, Signal b) {
            return Integer.compare(TimeframeUtil.rank(b.timeframe), TimeframeUtil.rank(a.timeframe));
        }
    };

    private final Context context;
    private final OnCardDismissListener dismissListener;
    private final List<String> symbols = new ArrayList<>();

    public SignalCardAdapter(Context context, OnCardDismissListener dismissListener) {
        this.context = context;
        this.dismissListener = dismissListener;
    }

    public void submitSymbols(List<String> newSymbols) {
        symbols.clear();
        symbols.addAll(newSymbols);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_signal_card, parent, false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        final String symbol = symbols.get(position);
        holder.symbolText.setText(symbol);

        List<Signal> active = SignalManager.getInstance().getActiveSignalsForSymbol(symbol);
        List<Signal> buys = new ArrayList<>();
        List<Signal> sells = new ArrayList<>();
        for (Signal s : active) {
            if ("Buy".equals(s.direction)) buys.add(s);
            else if ("Sell".equals(s.direction)) sells.add(s);
        }

        holder.capsuleContainer.removeAllViews();

        boolean hasBuy = !buys.isEmpty();
        boolean hasSell = !sells.isEmpty();

        if (hasBuy && hasSell) {
            // Split row: Sell group left, Buy group right.
            // Larger timeframes sit toward the outer wall on each side.
            Collections.sort(sells, BY_RANK_DESC); // largest first -> ends up near left (outer) wall
            Collections.sort(buys, BY_RANK_ASC);    // largest last -> ends up near right (outer) wall

            LinearLayout sellGroup = buildGroup(sells);
            sellGroup.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            sellGroup.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            LinearLayout buyGroup = buildGroup(buys);
            buyGroup.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            buyGroup.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            holder.capsuleContainer.addView(sellGroup);
            holder.capsuleContainer.addView(buyGroup);
        } else if (hasBuy) {
            Collections.sort(buys, BY_RANK_ASC);
            LinearLayout group = buildGroup(buys);
            group.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            group.setGravity(Gravity.END);
            holder.capsuleContainer.addView(group);
        } else if (hasSell) {
            Collections.sort(sells, BY_RANK_ASC);
            LinearLayout group = buildGroup(sells);
            group.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            group.setGravity(Gravity.START);
            holder.capsuleContainer.addView(group);
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dismissListener.onDismiss(symbol);
                return true;
            }
        });
    }

    private LinearLayout buildGroup(List<Signal> signalsForGroup) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.HORIZONTAL);
        for (Signal s : signalsForGroup) {
            View capsule = LayoutInflater.from(context).inflate(R.layout.capsule_signal, group, false);
            CountdownCircleView circle = capsule.findViewById(R.id.countdown_circle);
            TextView tfText = capsule.findViewById(R.id.timeframe_text);

            boolean isBuy = "Buy".equals(s.direction);
            int color = Color.parseColor(isBuy ? "#4CAF50" : "#F44336");
            circle.setTimes(s.receiveTime, s.expiryTime, color);
            tfText.setText(s.timeframe);

            group.addView(capsule);
        }
        return group;
    }

    @Override
    public int getItemCount() {
        return symbols.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final TextView symbolText;
        final LinearLayout capsuleContainer;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbol_text);
            capsuleContainer = itemView.findViewById(R.id.capsule_container);
        }
    }
}
