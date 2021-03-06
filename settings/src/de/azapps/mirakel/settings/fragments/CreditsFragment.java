package de.azapps.mirakel.settings.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.settings.R;

public class CreditsFragment extends Fragment {
    private static final String[][] LIBRARIES = {
        { "Gson", "Apache 2.0", "https://code.google.com/p/google-gson/" },
        { "Joda-Time", "Apache 2.0", "http://joda-time.sourceforge.net" },
        {
            "Android Change Log", "Apache 2.0",
            "https://code.google.com/p/android-change-log/"
        },
        { "ACRA", "Apache 2.0", "http://acra.ch" },
        {
            "HoloColorPicker", "Apache 2.0",
            "https://github.com/LarsWerkman/HoloColorPicker"
        },
        {
            "Progress Wheel", "",
            "https://github.com/Todd-Davies/ProgressWheel"
        },
        {
            "DateTimePicker Compatibility Library", "Apache 2.0",
            "https://github.com/flavienlaurent/datetimepicker"
        },
        { "Webicons", "CC-Attrib", "http://fairheadcreative.com/" },
        {
            "Android Donations Lib", "Apache 2.0",
            "https://github.com/dschuermann/android-donations-lib"
        },
        { "Changelog", "", "https://code.google.com/p/android-change-log/" }
    };
    private static final String[][] TRANSLATIONS = {
        { "Spanish", "macebal, sml" },
        { "French", "Ghost of Kendo, waghanza, npettiaux, benasse" },
        { "German", "Anatolij Zelenin, Georg Semmler, Patrik Kernstock" },
        { "Portuguese", "Sérgio Marques" },
        { "Russian", "Katy, Dmitry Derjavin" },
        { "Spanisch", "macebal, RaindropR", "Pablo Corbalán (@monofluor)" },
        { "Norwegian", "Jim-Stefhan Johansen" }, { "Slovenian", "mateju" },
        { "Arabic", "Rajaa Gutknecht" }, { "Czech", "sarimak" },
        { "Dutch", "Toon van Gerwen" },
        { "Italian", "Rajaa Gutknecht, fazen, Claudio Arseni" },
        { "Bulgarian", "Boriana Tcholakova" }, { "Polish", "mruwek" },
        { "Catalan", "sml" }
    };



    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_credits, null);
        final TextView creditTextHead = (TextView) rootView.findViewById(R.id.credits_text_head);
        creditTextHead.setText(Html
                               .fromHtml(getString(R.string.credits_text_head)));
        creditTextHead.setMovementMethod(LinkMovementMethod.getInstance());
        // Set Libraries
        final StringBuilder libs = new StringBuilder(LIBRARIES.length * 20);
        for (final String[] library : CreditsFragment.LIBRARIES) {
            libs.append("<a href=\"" + library[2] + "\"><b>" + library[0]
                        + "</b></a> (" + library[1] + ")<br />");
        }
        final TextView creditTextLibs = (TextView) rootView.findViewById(R.id.credits_libraries_text);
        creditTextLibs.setText(Html.fromHtml(libs.toString()));
        creditTextLibs.setMovementMethod(LinkMovementMethod.getInstance());
        // Set TRANSLATIONS
        final StringBuilder trans = new StringBuilder(TRANSLATIONS.length * 20);
        for (final String[] translation : CreditsFragment.TRANSLATIONS) {
            trans.append("<b>" + translation[0] + ": </b>" + translation[1]
                         + "<br/>");
        }
        final TextView creditTextTrans = (TextView) rootView.findViewById(R.id.credits_translations_text);
        creditTextTrans.setText(Html.fromHtml(trans.toString()));
        creditTextTrans.setMovementMethod(LinkMovementMethod.getInstance());
        final TextView creditTextLicense = (TextView) rootView.findViewById(R.id.credits_license_text);
        creditTextLicense.setText(Html
                                  .fromHtml(getString(R.string.credits_license)));
        creditTextLicense.setMovementMethod(LinkMovementMethod.getInstance());

        // links
        final ImageView imgGithub = (ImageView) rootView.findViewById(R.id.img_github);
        imgGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View fv) {
                Helpers.openURL(getActivity(), "https://github.com/MirakelX/mirakel-android/");
            }
        });

        final ImageView imgGPlus = (ImageView) rootView.findViewById(R.id.img_gplus);
        imgGPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.openURL(getActivity(), "https://plus.google.com/u/0/communities/110640831388790835840");
            }
        });

        final Button btnFeedback = (Button) rootView.findViewById(R.id.send_feedback);
        btnFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.contact(getActivity());
            }
        });
        return rootView;
    }

}
