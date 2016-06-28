package io.relayr.iotsmartphone.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import butterknife.ButterKnife;
import io.relayr.iotsmartphone.R;
import za.co.riggaroo.materialhelptutorial.TutorialItem;
import za.co.riggaroo.materialhelptutorial.tutorial.MaterialTutorialActivity;

//UNUSED
public class ActivityTutorial extends AppCompatActivity {

    private static final int REQUEST_CODE = 2376;

    public static void start(Context context) {
        Intent intent = new Intent(context, ActivityTutorial.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);
        loadTutorial();
    }

    public void loadTutorial() {
        Intent mainAct = new Intent(this, MaterialTutorialActivity.class);
        mainAct.putParcelableArrayListExtra(MaterialTutorialActivity.MATERIAL_TUTORIAL_ARG_TUTORIAL_ITEMS, getTutorialItems());
        startActivityForResult(mainAct, REQUEST_CODE);
    }

    private ArrayList<TutorialItem> getTutorialItems() {
        TutorialItem tutorialItem1 = new TutorialItem(R.string.tut_1_title, R.string.tut_1_subtitle,
                R.color.secondary, R.drawable.relayr, 0);

        TutorialItem tutorialItem2 = new TutorialItem(R.string.tut_2_title, R.string.tut_2_subtitle,
                R.color.primary, R.drawable.idea, 0);

        TutorialItem tutorialItem3 = new TutorialItem(R.string.tut_3_title, R.string.tut_3_subtitle,
                R.color.primaryDark, R.mipmap.logo, 0);

        ArrayList<TutorialItem> tutorialItems = new ArrayList<>();
        tutorialItems.add(tutorialItem1);
        tutorialItems.add(tutorialItem2);
        tutorialItems.add(tutorialItem3);

        return tutorialItems;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            ActivityMain.start(this);
            finish();
        }
    }
}
