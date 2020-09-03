package ru.getof.taxispb.interfaces;

import ru.getof.taxispb.utils.AlertDialogBuilder;

public interface AlertDialogEvent {
    void onAnswerDialog(AlertDialogBuilder.DialogResult result);

}
