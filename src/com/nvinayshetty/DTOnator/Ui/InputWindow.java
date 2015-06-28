/*
 * Copyright (C) 2015 Vinaya Prasad N
 *
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or
 *         (at your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful,
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *         GNU General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nvinayshetty.DTOnator.Ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.nvinayshetty.DTOnator.ActionListener.ContextMenuMouseListener;
import com.nvinayshetty.DTOnator.ClassCreator.ClassType;
import com.nvinayshetty.DTOnator.DtoCreators.DtoGenerationFactory;
import com.nvinayshetty.DTOnator.DtoCreators.FeedType;
import com.nvinayshetty.DTOnator.DtoCreators.FieldEncapsulationOptions;
import com.nvinayshetty.DTOnator.DtoCreators.FieldType;
import com.nvinayshetty.DTOnator.FeedValidator.InputFeedValidationFactory;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.*;
import java.util.EnumSet;

public class InputWindow extends JFrame {
    private PsiClass mClass;
    private Project project;
    private PsiFile mFile;

    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonOk;
    private JTextPane inputFeedText;
    private JLabel exceptionLabel;

    private JRadioButton createSeparateFile;
    private JRadioButton creteSingleFile;
    private JRadioButton makeFieldsPrivate;
    private JRadioButton pojoRadioButton;
    private JRadioButton gsonRadioButton;
    private JRadioButton provideSetter;
    private JRadioButton provideGetter;

    private ButtonGroup classTypeButtonGroup;
    private ButtonGroup feedTypeButtonGroup;
    private JScrollPane exceptionLoggerPane;


    public InputWindow(PsiClass mClass) {
        this.mClass = mClass;
        project = mClass.getProject();
        mFile = mClass.getContainingFile();
        setContentPane(contentPane);
        inputFeedText.getRootPane().setSize(500, 400);
        setSize(1000, 500);
        setTitle("Generate DTO");
        getRootPane().setDefaultButton(buttonOk);
        initButtons();
        initListeners();
        setDefaultConditions();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void setDefaultConditions() {
        exceptionLoggerPane.setVisible(false);
        setEncapsulationOptionsVisible(false);
        gsonRadioButton.setSelected(true);
        creteSingleFile.setSelected(true);
    }

    private void initListeners() {
        inputFeedText.addMouseListener(new ContextMenuMouseListener());
        inputFeedText.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        onOK();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        onCancel();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        makeFieldsPrivate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (makeFieldsPrivate.isSelected()) {
                    setEncapsulationOptionsVisible(true);
                    SetEncapsulationOptionsSelected(true);
                } else {
                    setEncapsulationOptionsVisible(false);
                }
            }
        });
    }

    private void SetEncapsulationOptionsSelected(boolean condition) {
        provideGetter.setSelected(condition);
        provideSetter.setSelected(condition);
    }

    private void setEncapsulationOptionsVisible(boolean condition) {
        provideGetter.setVisible(condition);
        provideSetter.setVisible(condition);
    }

    private void initButtons() {
        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        classTypeButtonGroup = new ButtonGroup();
        classTypeButtonGroup.add(createSeparateFile);
        classTypeButtonGroup.add(creteSingleFile);
        feedTypeButtonGroup = new ButtonGroup();
        feedTypeButtonGroup.add(pojoRadioButton);
        feedTypeButtonGroup.add(gsonRadioButton);
    }


    private void onOK() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final Notification processingNotification = new Notification("DtoGenerator", "Dto generation in Progress", "please wait, it may takes few seconds to generate Dto depending on length of the feed", NotificationType.INFORMATION);
                processingNotification.notify(project);
                InputFeedValidationFactory validator = new InputFeedValidationFactory(getFeedType());
                if (validator.isValidFeed(inputFeedText.getText(), exceptionLoggerPane, exceptionLabel)) {
                    dispose();
                    WriteCommandAction writeAction = DtoGenerationFactory.getDtoGeneratorFor(getFeedType(), getClassTypePreference(), getFieldTYpe(), getFieldEncapsulationOptions(), project, mClass.getContainingFile(), (JSONObject) validator.getValidFeed(), mClass);
                    writeAction.execute();
                    processingNotification.expire();
                }
            }
        });
    }

    private FieldType getFieldTYpe() {
        if (pojoRadioButton.isSelected())
            return FieldType.POJO;
        else
            return FieldType.GSON;
    }

    private FeedType getFeedType() {
        //Todo:implement Xml support
        return FeedType.JSON;
    }

    private EnumSet<FieldEncapsulationOptions> getFieldEncapsulationOptions() {
        EnumSet<FieldEncapsulationOptions> fieldEncapsulationOptions = EnumSet.noneOf(FieldEncapsulationOptions.class);
        if (makeFieldsPrivate.isSelected())
            fieldEncapsulationOptions.add(FieldEncapsulationOptions.PROVIDE_PRIVATE_FIELD);
        if (provideGetter.isSelected())
            fieldEncapsulationOptions.add(FieldEncapsulationOptions.PROVIDE_GETTER);
        if (provideSetter.isSelected())
            fieldEncapsulationOptions.add(FieldEncapsulationOptions.PROVIDE_SETTER);
        return fieldEncapsulationOptions;
    }

    private ClassType getClassTypePreference() {
        if (creteSingleFile.isSelected())
            return ClassType.SINGLE_FILE_WITH_INNER_CLASS;
        else
            return ClassType.SEPARATE_FILE;
    }


    private void onCancel() {
        dispose();
    }


}
