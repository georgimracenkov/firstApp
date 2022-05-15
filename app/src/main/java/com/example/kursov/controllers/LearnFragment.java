package com.example.kursov.controllers;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.kursov.R;

import java.util.Arrays;

public class LearnFragment extends Fragment {
    String[] titles;
    TableLayout titlesTable;
    AutoCompleteTextView searchTextView;

    public LearnFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frView = inflater.inflate(R.layout.fragment_learn, container, false);

        searchTextView = frView.findViewById(R.id.searchAutoCompleteTextView);
        titlesTable = frView.findViewById(R.id.titlesTable);
        ImageView search = frView.findViewById(R.id.learnSearchBtn);

        titles = getResources().getStringArray(R.array.titlesArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, titles);
        searchTextView.setAdapter(adapter);

        fillTableLayoutWithTitles(titles);

        search.setOnClickListener(view -> {
            search(searchTextView.getText().toString(), frView);
        });

        return frView;
    }

    private void getInformation(String title) {
        Intent i = new Intent(getActivity(), InformationActivity.class);
        i.putExtra("title", title);
        startActivity(i);
    }

    private void search(String search, View frView) {
        if (!search.isEmpty() && !Arrays.asList(titles).contains(search)) {
            showNoResultsFoundDialog(frView);
        } else if (Arrays.asList(titles).contains(search)){
            titlesTable.removeAllViews();
            titlesTable.addView(setTableRow(search));
        } else if (searchTextView.getText().toString().isEmpty()) {
            fillTableLayoutWithTitles(titles);
        }
    }

    private void fillTableLayoutWithTitles(String[] titles) {
        titlesTable.removeAllViews();
        for (String title : titles) {
            titlesTable.addView(setTableRow(title));
        }
    }

    private TextView setTableRow(String title) {
        TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(titlesTable.getLayoutParams());
        tableRowParams.setMargins(0, 10, 0, 10);
        TextView titleTextView = new TextView(getActivity());
        titleTextView.setText(title);
        titleTextView.setTextSize(40);
        titleTextView.setPadding(30, 10, 30, 10);
        titleTextView.setBackgroundResource(R.drawable.score_textview_border);
        titleTextView.setTextColor(getResources().getColor(R.color.white, getActivity().getTheme()));
        titleTextView.setOnClickListener(view -> getInformation(title));
        titleTextView.setLayoutParams(tableRowParams);

        return titleTextView;
    }

    private void showNoResultsFoundDialog(View frView) {
        AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        View view = LayoutInflater.from(getActivity()).inflate(
                R.layout.layout_error_dialog,
                frView.findViewById(R.id.layoutDialogContainer)
        );

        builder.setView(view);
        builder.setCancelable(false);
        ((TextView) view.findViewById(R.id.textTitle)).setText(getResources().getString(R.string.dialog_error_title));
        ((TextView) view.findViewById(R.id.textMessage)).setText(R.string.no_results_found_message);
        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(R.drawable.ic_error);
        Button okBtn = view.findViewById(R.id.buttonAction);
        okBtn.setText(R.string.okay);

        AlertDialog alertDialog = builder.create();

        okBtn.setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }
}