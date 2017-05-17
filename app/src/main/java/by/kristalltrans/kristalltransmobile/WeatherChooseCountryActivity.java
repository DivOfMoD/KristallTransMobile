package by.kristalltrans.kristalltransmobile;

import android.content.Intent;
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

public class WeatherChooseCountryActivity extends AppCompatActivity {

    static final private int REQUEST_CODE_CHOOSE_CITY = 0;

    ArrayList<String> listCountries;

    String[] items;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;
    ListView listView;
    EditText editText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_choose_region);

        listCountries = new ArrayList<>();
        try {
            XmlPullParser parser = getResources().getXml(R.xml.cities);

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {

                if (parser.getEventType() == XmlPullParser.START_TAG) {

                    if (parser.getName().equals("country") && parser.getDepth() == 2 && parser.getAttributeCount() == 1)
                        listCountries.add(parser.getAttributeValue(0));
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
                startActivityForResult(new Intent(WeatherChooseCountryActivity.this, WeatherChooseCityActivity.class).putExtra("country", ((TextView) itemClicked).getText().toString()), REQUEST_CODE_CHOOSE_CITY);
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
        items = listCountries.toArray(new String[listCountries.size()]);
        listItems = new ArrayList<>(Arrays.asList(items));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_CITY) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, new Intent());
                finish();
            }
        }
    }
}