package me.sphere.appcore.utils

fun <R, A, B> zip(p1: A?, p2: B?, transform: (p1: A, p2: B) -> R): R?
    = p1?.let { p2?.let { transform(p1, p2) } }

fun <R, A, B, C> zip(p1: A?, p2: B?, p3: C?, transform: (p1: A, p2: B, p3: C) -> R): R?
    = p1?.let { p2?.let { p3?.let { transform(p1, p2, p3) } } }

fun <R, A, B, C, D> zip(p1: A?, p2: B?, p3: C?, p4: D?, transform: (p1: A, p2: B, p3: C, p4: D) -> R): R?
    = p1?.let { p2?.let { p3?.let { p4?.let { transform(p1, p2, p3, p4) } } } }

fun <R, A, B, C, D, E> zip(p1: A?, p2: B?, p3: C?, p4: D?, p5: E?, transform: (p1: A, p2: B, p3: C, p4: D, p5: E) -> R): R?
    = p1?.let { p2?.let { p3?.let { p4?.let { p5?.let { transform(p1, p2, p3, p4, p5) } } } } }
