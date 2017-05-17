package by.kristalltrans.kristalltransmobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Arrays;

public class WeatherChooseCityActivity extends AppCompatActivity {

    ArrayList<String[]> listCities;

    String[] items;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;
    ListView listView;
    EditText editText;

    public static final String APP_PREFERENCES = "by.kristalltrans.kristalltransmobile";
    public static final String APP_PREFERENCES_COUNTER = "region";
    private SharedPreferences mSettings;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_choose_region);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        listCities = new ArrayList<>();
        try {
            XmlPullParser parser = getResources().getXml(R.xml.cities);

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {

                if (parser.getEventType() == XmlPullParser.START_TAG) {

                    if (parser.getName().equals("city") && parser.getDepth() == 3 && parser.getAttributeCount() == 8) {

                        String[] arr = new String[3];

                        arr[0] = parser.getAttributeValue(1);
                        arr[1] = parser.getAttributeValue(4);
                        parser.next();
                        arr[2] = parser.getText();

                        listCities.add(arr);
                    }
                }
                parser.next();
            }
        } catch (Throwable t) {
            finish();
        }

        listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                ArrayList<String> regions = new ArrayList<>();
                for (int i = 0; i < listCities.size(); i++)
                    if (listCities.get(i)[2].equals(((TextView) itemClicked).getText()))
                        regions.add(listCities.get(i)[0]);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString(APP_PREFERENCES_COUNTER, regions.get(0));
                editor.apply();
                setResult(RESULT_OK, new Intent());
                finish();
            }
        });
        editText = (EditText) findViewById(R.id.txtsearch);
        initList();

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals("")) {
                    // reset listview
                    initList();
                } else {
                    // perform search
                    searchItem(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });

    }

    public void searchItem(String textToSearch) {
        for (String item : items) {
            if (!item.contains(textToSearch)) {
                listItems.remove(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void initList() {
        ArrayList<String> citiesNames = new ArrayList<>();
        for (int i = 0; i < listCities.size(); i++)
            if (listCities.get(i)[1].equals(getIntent().getExtras().getString("country")))
                citiesNames.add(listCities.get(i)[2]);

        items = citiesNames.toArray(new String[citiesNames.size()]);
        listItems = new ArrayList<>(Arrays.asList(items));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        listView.setAdapter(adapter);
    }

}
