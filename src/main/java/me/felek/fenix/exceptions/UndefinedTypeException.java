package me.felek.fenix.exceptions;

/**
 * @className: UndefinedTypeException
 * @author: Felek
 * @date: 27/10/2026 15:08
 */

public class UndefinedTypeException extends RuntimeException {
    public UndefinedTypeException(String type) {
        super("Undefined type " + type);
    }
}
