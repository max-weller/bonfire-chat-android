package de.tudarmstadt.informatik.bp.bonfirechat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.List;

import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireData;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.UIHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.zxing.IntentIntegrator;
import de.tudarmstadt.informatik.bp.bonfirechat.location.GpsTracker;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Contact;
import de.tudarmstadt.informatik.bp.bonfirechat.R;

/**
 * contacts list
 */
public class ContactsFragment extends Fragment {

    private ContactsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (UIHelper.shouldShowContactsTutorial(getActivity())) {
            showcaseView = new ShowcaseView.Builder(getActivity())
                    .setStyle(R.style.CustomShowcaseTheme2)
                    .setOnClickListener(showcaseListener)
                    .build();
            tutorial_counter = 0;
            showcaseListener.onClick(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        BonfireData db = BonfireData.getInstance(getActivity());
        List<Contact> contacts = db.getContacts();
        adapter = new ContactsAdapter(this.getActivity(), contacts);
        contactsList.setAdapter(adapter);
    }


    ListView contactsList;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);

        contactsList = (ListView) rootView.findViewById(R.id.contactsList);

        contactsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        contactsList.setMultiChoiceModeListener(multiChoiceListener);

        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // open up messages
                MessagesActivity.startConversationWithPeer(ContactsFragment.this.getActivity(), adapter.getItem(position));
            }
        });

        return rootView;
    }
    // ####### First-Start Tutorial #####################################################
    private ShowcaseView showcaseView;
    private int tutorial_counter = 0;
    /**
     * Handles clicks on Close button of first-start tutorial view
     */
    private View.OnClickListener showcaseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch(tutorial_counter++) {
                case 0:
                    showcaseView.setContentTitle("Deine Kontaktdaten");
                    showcaseView.setContentText("Hier klicken, um deine Kontaktdaten weiterzugeben");
                    showcaseView.setTarget(new ActionItemTarget(ContactsFragment.this.getActivity(), R.id.action_scan_nfc));
                    showcaseView.setButtonText("Weiter");
                    break;
                case 1:
                    showcaseView.setContentTitle("QR-Code scannen");
                    showcaseView.setContentText("Hier klicken, um einen QR-Code zu scannen, um einen Kontakt hinzuzufügen");
                    showcaseView.setTarget(new ActionItemTarget(ContactsFragment.this.getActivity(), R.id.action_scan_qr));
                    showcaseView.setButtonText("Weiter");
                    break;
                case 2:
                    showcaseView.setContentTitle("Kontakt suchen");
                    showcaseView.setContentText("Hier klicken, um einen Kontakt über das Internet zu suchen");
                    showcaseView.setTarget(new ActionItemTarget(ContactsFragment.this.getActivity(), R.id.action_search));
                    showcaseView.setButtonText("Alles klar");
                    break;
                case 3:
                    UIHelper.flagShownContactsTutorial(getActivity());
                    showcaseView.hide();
                    break;
            }
        }
    };
    // ####### End First-Start Tutorial #####################################################

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contacts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        final BonfireData bonfireData = BonfireData.getInstance(adapter.getContext());

        if (item.getItemId() == R.id.action_search) {
            startActivity(new Intent(getActivity(), SearchUserActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_scan_qr) {
            IntentIntegrator inte = new IntentIntegrator(getActivity());
            inte.initiateScan();
        } else if (item.getItemId() == R.id.action_scan_nfc) {
            startActivity(new Intent(getActivity(), ShareMyIdentityActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    private void deleteSelectedItems() {
        BonfireData db = BonfireData.getInstance(getActivity());
        boolean[] mySelected = adapter.itemSelected;

        for (int position = adapter.getCount() - 1; position >= 0; position--) {
            if (mySelected[position]) {
                db.deleteContact(adapter.getItem(position));
                adapter.remove(adapter.getItem(position));
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void detailsForSelectedItems() {
        boolean[] mySelected = adapter.itemSelected;

        for (int position = adapter.getCount() - 1; position >= 0; position--) {
            if (mySelected[position]) {
                Contact contact = adapter.getItem(position);
                Intent intent = new Intent(getActivity(), ContactDetailsActivity.class);
                intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT_ID, contact.rowid);
                startActivity(intent);
                break;
            }
        }
    }

    private AbsListView.MultiChoiceModeListener multiChoiceListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB
            adapter.itemSelected[position] = checked;
            adapter.notifyDataSetChanged();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.action_contact_details:
                    detailsForSelectedItems();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contact, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.itemSelected = new boolean[adapter.getCount()];
            adapter.notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            return false;
        }
    };


}