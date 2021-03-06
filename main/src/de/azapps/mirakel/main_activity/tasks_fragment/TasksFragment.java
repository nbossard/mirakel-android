/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity.tasks_fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.DefenitionsModel.ExecInterfaceWithTask;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskVanishedException;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

import static de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation.LIKE;
import static de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting.ASC;

public class TasksFragment extends Fragment implements
    LoaderManager.LoaderCallbacks<Cursor>, FilterQueryProvider {
    private static final String TAG = "TasksFragment";
    private static final int TASK_RENAME = 0, TASK_MOVE = 1, TASK_DESTROY = 2;
    protected TaskAdapter adapter;
    protected boolean created = false;
    protected long listId;
    protected ListView listView;
    protected ActionMode mActionMode = null;
    protected MainActivity main;

    protected EditText newTask;

    View view;

    public TasksFragment() {
        super();
    }

    public void clearFocus() {
        if (this.newTask != null) {
            this.newTask.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (TasksFragment.this.newTask == null) {
                        return;
                    }
                    TasksFragment.this.newTask.setOnFocusChangeListener(null);
                    TasksFragment.this.newTask.clearFocus();
                    if (getActivity() == null) {
                        return;
                    }
                    final InputMethodManager imm = (InputMethodManager) getActivity()
                                                   .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(
                        TasksFragment.this.newTask.getWindowToken(), 0);
                    getActivity().getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                }
            }, 10);
        }
    }

    public void focusNew(final boolean request_focus) {
        if (this.newTask == null) {
            return;
        }
        this.newTask.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                if (TasksFragment.this.main.getCurrentPosition() != MainActivity
                    .getTasksFragmentPosition()) {
                    return;
                }
                TasksFragment.this.newTask.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null) {
                            // What the fuck?? This should not happen but it happens for some users
                            return;
                        }
                        final InputMethodManager imm = (InputMethodManager) getActivity()
                                                       .getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm == null) {
                            Log.w(TasksFragment.TAG, "Inputmanager==null");
                            return;
                        }
                        imm.restartInput(TasksFragment.this.newTask);
                        if (request_focus && hasFocus) {
                            getActivity()
                            .getWindow()
                            .setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                            imm.showSoftInput(TasksFragment.this.newTask,
                                              InputMethodManager.SHOW_IMPLICIT);
                            getActivity()
                            .getWindow()
                            .setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        } else if (!hasFocus) {
                            TasksFragment.this.newTask.requestFocus();
                            imm.showSoftInput(TasksFragment.this.newTask,
                                              InputMethodManager.SHOW_IMPLICIT);
                        } else {
                            clearFocus();
                        }
                    }
                });
            }
        });
        this.newTask.requestFocus();
        if (!this.newTask.hasFocus()) {
            this.newTask.postDelayed(new Runnable() {
                @Override
                public void run() {
                    TasksFragment.this.newTask.requestFocus();
                    Log.wtf(TAG, "second try");
                }
            }, 10);
        }
    }

    public TaskAdapter getAdapter() {
        return this.adapter;
    }

    public View getFragmentView() {
        return this.view;
    }

    public ListView getListView() {
        return this.listView;
    }

    public boolean isReady() {
        return this.newTask != null;
    }

    protected boolean newTask(final String name) {
        this.newTask.setText(null);
        final InputMethodManager imm = (InputMethodManager) this.main
                                       .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.newTask.getWindowToken(), 0);
        this.newTask.clearFocus();
        if (name.equals("")) {
            this.newTask.setOnFocusChangeListener(null);
            imm.showSoftInput(this.newTask,
                              InputMethodManager.HIDE_IMPLICIT_ONLY);
            return true;
        }
        final ListMirakel list = this.main.getCurrentList();
        final Task createdTask = Semantic.createTask(name, Optional.fromNullable(list),
                                 true, getActivity());
        getLoaderManager().restartLoader(0, null, this);
        this.main.setCurrentTask(createdTask, false);
        this.main.getListFragment().update();
        if (!MirakelCommonPreferences.hideKeyboard()) {
            focusNew(true);
        }
        this.main.updateShare();
        return true;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.main = (MainActivity) getActivity();
        this.listId = this.main.getCurrentList().getId();
        this.view = inflater.inflate(R.layout.layout_tasks_fragment, container,
                                     false);
        this.adapter = null;
        this.created = true;
        this.listView = (ListView) this.view.findViewById(R.id.tasks_list);
        this.listView
        .setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // Events
        this.newTask = (EditText) this.view.findViewById(R.id.tasks_new);
        if (MirakelCommonPreferences.isTablet()) {
            this.newTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        }
        this.newTask.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId,
                                          final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                    newTask(v.getText().toString());
                    v.setText(null);
                }
                return false;
            }
        });
        this.newTask.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                final ImageButton send = (ImageButton) TasksFragment.this.view
                                         .findViewById(R.id.btnEnter);
                if (s.length() > 0) {
                    send.setVisibility(View.VISIBLE);
                } else {
                    send.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(final CharSequence s,
                                          final int start, final int count, final int after) {
                // Nothing
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                // Nothing
            }
        });
        update(true);
        final ImageButton btnEnter = (ImageButton) this.view
                                     .findViewById(R.id.btnEnter);
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                newTask(TasksFragment.this.newTask.getText().toString());
                TasksFragment.this.newTask.setText(null);
            }
        });
        updateButtons();
        return this.view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void setActivity(final MainActivity activity) {
        this.main = activity;
    }

    public void setScrollPosition(final int pos) {
        if (this.listView == null || this.main == null) {
            return;
        }
        this.main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TasksFragment.this.listView.getCount() > pos) {
                    TasksFragment.this.listView.setSelectionFromTop(pos, 0);
                } else {
                    TasksFragment.this.listView.setSelectionFromTop(0, 0);
                }
            }
        });
    }

    @SuppressLint("NewApi")
    protected void update(final boolean reset) {
        if (!this.created) {
            return;
        }
        this.listView = (ListView) this.view.findViewById(R.id.tasks_list);
        this.adapter = new TaskAdapter(getActivity(),
        new OnTaskChangedListner() {
            @Override
            public void onTaskChanged(final Task newTask) {
                getLoaderManager().restartLoader(0, null,
                                                 TasksFragment.this);
                if (MirakelCommonPreferences.isTablet()
                    && TasksFragment.this.main.getTaskFragment() != null
                    && TasksFragment.this.main.getTaskFragment()
                    .getTask().getId() == newTask.getId()) {
                    TasksFragment.this.main.getTaskFragment().update(
                        newTask);
                }
            }
        });
        adapter.setFilterQueryProvider(this);
        this.listView.setAdapter(this.adapter);
        getLoaderManager().initLoader(0, null, this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            this.listView
            .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(
                    final AdapterView<?> parent, final View item,
                    final int position, final long id) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                    final Task task = Task.get((Long) item.getTag()).orNull();
                    if (task == null) {
                        return false;
                    }
                    builder.setTitle(task.getName());
                    final List<CharSequence> items = new ArrayList<CharSequence>(
                        Arrays.asList(getActivity().getResources()
                                      .getStringArray(
                                          R.array.task_actions_items)));
                    builder.setItems(items
                                     .toArray(new CharSequence[items.size()]),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int item) {
                            switch (item) {
                            case TASK_RENAME:
                                TasksFragment.this.main
                                .setCurrentTask(task);
                                break;
                            case TASK_MOVE:
                                TasksFragment.this.main
                                .handleMoveTask(task);
                                break;
                            case TASK_DESTROY:
                                TasksFragment.this.main
                                .handleDestroyTask(task);
                                break;
                            default:
                                break;
                            }
                        }
                    });
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }
            });
        } else {
            this.listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
            this.listView.setHapticFeedbackEnabled(true);
            this.listView
            .setMultiChoiceModeListener(new MultiChoiceModeListener() {
                @Override
                public boolean onActionItemClicked(
                    final ActionMode mode, final MenuItem item) {
                    switch (item.getItemId()) {
                    case R.id.menu_delete:
                        TasksFragment.this.main
                        .handleDestroyTask(TasksFragment.this.selectedTasks);
                        break;
                    case R.id.menu_move:
                        TasksFragment.this.main
                        .handleMoveTask(TasksFragment.this.selectedTasks);
                        break;
                    case R.id.menu_set_due:
                        TasksFragment.this.main
                        .handleSetDue(TasksFragment.this.selectedTasks);
                        break;
                    case R.id.edit_task:
                        TasksFragment.this.main.setCurrentTask(
                            TasksFragment.this.selectedTasks.get(0),
                            true);
                        break;
                    case R.id.done_task:
                        for (final Task t : TasksFragment.this.selectedTasks) {
                            t.setDone(true);
                            t.save();
                        }
                        getLoaderManager().restartLoader(0, null,
                                                         TasksFragment.this);
                        break;
                    default:
                        break;
                    }
                    mode.finish();
                    return false;
                }

                @Override
                public boolean onCreateActionMode(
                    final ActionMode mode, final Menu menu) {
                    final MenuInflater inflater = mode
                                                  .getMenuInflater();
                    inflater.inflate(R.menu.context_tasks, menu);
                    TasksFragment.this.mActionMode = mode;
                    clearFocus();
                    TasksFragment.this.selectedTasks = new ArrayList<>();
                    return true;
                }

                @Override
                public void onDestroyActionMode(final ActionMode mode) {
                    TasksFragment.this.selectedTasks = new ArrayList<>();
                }

                @Override
                public void onItemCheckedStateChanged(
                    final ActionMode mode, final int position,
                    final long id, final boolean checked) {
                    final Cursor cursor = (Cursor) TasksFragment.this.listView
                                          .getItemAtPosition(position);
                    if (cursor.getCount() > 0) {
                        final Task t = new Task(cursor);
                        if (!TasksFragment.this.selectedTasks
                            .contains(t) && checked) {
                            TasksFragment.this.selectedTasks.add(t);
                        } else if (checked) {
                            TasksFragment.this.selectedTasks.remove(t);
                        }
                    }
                }

                @Override
                public boolean onPrepareActionMode(
                    final ActionMode mode, final Menu menu) {
                    menu.findItem(R.id.edit_task)
                    .setVisible(
                        TasksFragment.this.selectedTasks
                        .size() <= 1);
                    return false;
                }
            });
        }
        this.listView
        .setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent,
                                    final View item, final int position, final long id) {
                long taskId = (Long) item.getTag();
                Optional<Task> taskOptional = Task.get(taskId);
                if (taskOptional.isPresent()) {
                    TasksFragment.this.main.setCurrentTask(taskOptional.get(), true);
                } else {
                    throw new TaskVanishedException(taskId);
                }
            }
        });
    }

    protected List<Task> selectedTasks;

    public void search(final String query) {
        adapter.getFilter().filter(query);
    }

    public void updateButtons() {
        // a) Android 2.3 dosen't support speech toText
        // b) The user can switch off the button
        if (this.view == null) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.HONEYCOMB
            || !MirakelCommonPreferences.useBtnSpeak()) {
            this.view.findViewById(R.id.btnSpeak_tasks)
            .setVisibility(View.GONE);
        } else {
            final ImageButton btnSpeak = (ImageButton) this.view
                                         .findViewById(R.id.btnSpeak_tasks);
            // txtText = newTask;
            btnSpeak.setVisibility(View.VISIBLE);
            btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    TasksFragment.this.main
                                    .getString(R.string.speak_lang_code));
                    try {
                        getActivity().startActivityForResult(intent,
                                                             MainActivity.RESULT_SPEECH);
                        TasksFragment.this.newTask.setText("");
                    } catch (final ActivityNotFoundException a) {
                        ErrorReporter.report(ErrorType.NO_SPEACH_RECOGNITION);
                    }
                }
            });
        }
        if (!MirakelCommonPreferences.useBtnAudioRecord()) {
            this.view.findViewById(R.id.btnAudio_tasks)
            .setVisibility(View.GONE);
        } else {
            final ImageButton btnAudio = (ImageButton) this.view
                                         .findViewById(R.id.btnAudio_tasks);
            btnAudio.setVisibility(View.VISIBLE);
            btnAudio.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // TODO BAHHHH this is ugly!
                    final Task task = Task.getDummy(getActivity(), TasksFragment.this.main.getCurrentList());
                    TaskDialogHelpers.handleAudioRecord(
                        TasksFragment.this.main, task,
                    new ExecInterfaceWithTask() {
                        @Override
                        public void exec(final Task t) {
                            TasksFragment.this.main.setCurrentList(t
                                                                   .getList());
                            TasksFragment.this.main.setCurrentTask(t,
                                                                   true);
                        }
                    });
                }
            });
        }
        if (!MirakelCommonPreferences.useBtnCamera()
            || !Helpers.isIntentAvailable(this.main,
                                          MediaStore.ACTION_IMAGE_CAPTURE)) {
            this.view.findViewById(R.id.btnCamera).setVisibility(View.GONE);
        } else {
            final ImageButton btnCamera = (ImageButton) this.view
                                          .findViewById(R.id.btnCamera);
            btnCamera.setVisibility(View.VISIBLE);
            btnCamera.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    try {
                        final Intent cameraIntent = new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE);
                        final Uri fileUri = FileUtils
                                            .getOutputMediaFileUri(FileUtils.MEDIA_TYPE_IMAGE);
                        if (fileUri == null) {
                            return;
                        }
                        TasksFragment.this.main.setFileUri(fileUri);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                        getActivity().startActivityForResult(cameraIntent,
                                                             MainActivity.RESULT_CAMERA);
                    } catch (final ActivityNotFoundException a) {
                        ErrorReporter.report(ErrorType.PHOTO_NO_CAMERA);
                    } catch (final IOException e) {
                        if (e.getMessage().equals(FileUtils.ERROR_NO_MEDIA_DIR)) {
                            ErrorReporter
                            .report(ErrorType.PHOTO_NO_MEDIA_DIRECTORY);
                        }
                    }
                }
            });
        }
    }

    public void updateList() {
        if (this.main == null || this.main.getCurrentList() == null) {
            Log.wtf(TAG, "no current list found");
            return;
        }
        this.listId = this.main.getCurrentList().getId();
        this.main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    getLoaderManager().restartLoader(0, null,
                                                     TasksFragment.this);
                } catch (final Exception e) {
                    // TODO fix try catch
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int arg0, final Bundle arg1) {
        return getList().getTasksQueryBuilder().toSupportCursorLoader(Task.URI);
    }

    private ListMirakel getList() {
        Optional<ListMirakel> listMirakelOptional = ListMirakel.get(listId);

        if (!listMirakelOptional.isPresent()) {
            ErrorReporter.report(ErrorType.LIST_VANISHED);
            return SpecialList.firstSpecialSafe();
        } else {
            return listMirakelOptional.get();
        }
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader,
                               final Cursor newCursor) {
        this.adapter.swapCursor(newCursor);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        this.adapter.swapCursor(null);
    }


    public View getViewForTask(final Task task) {
        return getListView().findViewWithTag(task.getId());
    }

    public void hideActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        ListMirakel list = getList();
        MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(getActivity());
        Task.addBasicFiler(mirakelQueryBuilder);
        mirakelQueryBuilder.and(ModelBase.NAME, LIKE, "%" + constraint + "%");
        if (list.isSpecial()) {
            MirakelQueryBuilder sortQuery = list.getWhereQueryForTasks().select(ModelBase.ID);
            mirakelQueryBuilder.sort("CASE WHEN " + ModelBase.ID + " IN (" + sortQuery.getQuery(
                                         Task.URI) + ") THEN 0  ELSE 1 END", ASC, sortQuery.getSelectionArguments());
        } else {
            mirakelQueryBuilder.sort("CASE WHEN " + Task.LIST_ID + " = " + list.getId() + " THEN 0 ELSE 1 END",
                                     ASC);
        }
        list.addSortBy(mirakelQueryBuilder);
        return mirakelQueryBuilder.query(Task.URI);
    }
}
