package com.liftric.auth.base

expect internal class Environment() {
    companion object {
        /**
         * Accesses environment variable
         * @return Variable as string
         */
        fun variable(value: String): String?
    }
}