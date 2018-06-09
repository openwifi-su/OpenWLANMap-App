package su.openwifi.openwlanmap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        //get view
        ListView listRank = findViewById(R.id.list_ranking);
        TextView userRank = findViewById(R.id.user_ranking);

        //test data
        userRank.setText("15");
        List<Rank> list = new ArrayList<>();
        list.add(new Rank("test team 1", 111000, 1));
        list.add(new Rank("test team 2", 11100, 2));
        list.add(new Rank("test team 3", 10000, 3));
        list.add(new Rank("test team 4", 7000, 4));
        list.add(new Rank("test team 5", 5000, 5));
        list.add(new Rank("test team 6", 4900, 6));
        list.add(new Rank("test team 7", 3000, 7));
        list.add(new Rank("test team 8", 2800, 8));
        RankAdapter adapter = new RankAdapter(this, list);
        listRank.setAdapter(adapter);
    }

    public class Rank {
        protected String name;
        protected int pos;
        protected int point;

        public Rank(String name, int point, int pos) {
            this.name = name;
            this.point = point;
            this.pos = pos;
        }
    }

    public class RankAdapter extends ArrayAdapter<Rank> {
        public RankAdapter(@NonNull Context context, List<Rank> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.rank_layout, parent, false);
            }
            Rank rank = getItem(position);

            TextView name = view.findViewById(R.id.name);
            TextView pos = view.findViewById(R.id.position);
            TextView point = view.findViewById(R.id.point);

            name.setText(rank.name);
            pos.setText(String.valueOf(rank.pos));
            point.setText(String.valueOf(rank.point));
            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
