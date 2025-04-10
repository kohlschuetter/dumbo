/*
 * Copyright 2022-2025 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.dumbo.util;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Map;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;

/**
 * An {@link Appendable} that multiplexes calls to zero or more {@link Appendable}s.
 *
 * @author Christian Kohlschütter
 * @see SuppressErrorsAppendable a good subclass
 */
public class MultiplexedAppendable implements Appendable {
  final Appendable[] targets;
  private final BitSet excluded;

  /**
   * Creates a new {@link MultiplexedAppendable} for the given targets. Entries that are
   * {@code null} are automatically ignored. The list of targets is effectively empty, this instance
   * acts as a dummy {@link Appendable} that does not trigger any error.
   *
   * @param targets The list of targets.
   */
  @SuppressWarnings("PMD.ArrayIsStoredDirectly")
  public MultiplexedAppendable(Appendable... targets) {
    this.targets = targets;
    this.excluded = new BitSet(targets.length);
  }

  protected static final Appendable checkIfOnlyOneNonNull(Appendable... targets) {
    int nonNull = 0;
    Appendable firstNonNull = null;
    for (Appendable target : targets) {
      if (target != null) {
        if (nonNull++ == 0) {
          firstNonNull = target;
        } else {
          return null;
        }
      }
    }
    return firstNonNull;
  }

  /**
   * Returns a {@link MultiplexedAppendable} for the given targets, unless there's only one non-null
   * {@link Appendable} in the array, in which case that one is returned.
   *
   * @param targets The targets.
   * @return An {@link Appendable} that either is a {@link MultiplexedAppendable}, or not.
   */
  public static Appendable multiplexIfNecessary(Appendable... targets) {
    Appendable onlyOne = checkIfOnlyOneNonNull(targets);
    if (onlyOne != null) {
      return onlyOne;
    } else {
      return new MultiplexedAppendable(targets);
    }
  }

  /**
   * A {@link MultiplexedAppendable} that suppresses exceptions when they are thrown upon
   * {@code append}, for them to be checked later if necessary.
   *
   * This is particularly useful if you're generating some content, and you want the caller to not
   * handle exceptions if one out of multiple {@link Appendable}s has a problem.
   *
   * Use {@link #checkError()}, {@link #checkError(Appendable)}, {@link #hasError()},
   * {@link #hasError(Appendable)}, {@link #getFirstError()}, {@link #getError(Appendable)},
   * {@link #getFirstAppendableWithError()}, {@link #getFirstErrorWithAppendable()} to check/handle
   * exceptions.
   *
   * @author Christian Kohlschütter
   */
  public static final class SuppressErrorsAppendable extends MultiplexedAppendable {
    private final Throwable[] errors;

    /**
     * Creates a new {@link SuppressErrorsAppendable} for the given targets. Entries that are
     * {@code null} are automatically ignored. The list of targets is effectively empty, this
     * instance acts as a dummy {@link Appendable} that does not trigger any error.
     *
     * @param targets The list of targets.
     */
    public SuppressErrorsAppendable(Appendable... targets) {
      super(targets);
      this.errors = new Throwable[targets.length];
    }

    /**
     * Returns a {@link SuppressErrorsAppendable} for the given targets, unless there's only one
     * non-null {@link Appendable} in the array, in which case that one is returned.
     *
     * @param targets The targets.
     * @return An {@link Appendable} that either is a {@link SuppressErrorsAppendable}, or not.
     */
    @SuppressFBWarnings("HSM_HIDING_METHOD")
    public static Appendable multiplexIfNecessary(Appendable... targets) {
      Appendable onlyOne = checkIfOnlyOneNonNull(targets);
      if (onlyOne != null) {
        return onlyOne;
      } else {
        return new MultiplexedAppendable.SuppressErrorsAppendable(targets);
      }
    }

    private void setThrowable(Appendable target, int index, Throwable t) {
      if (targets[index] == target) { // NOPMD
        if (errors[index] == null) {
          errors[index] = t;
        }
      }
    }

    /**
     * Called upon encountering an error (an {@link IOException}, {@link RuntimeException} or
     * {@link Error}) during {@link #append(char)}, {@link #append(CharSequence)} or
     * {@link #append(CharSequence, int, int)}.
     *
     * In this implementation, the error is not thrown immediately, but recorded, and optionally
     * thrown later via {@link #checkError()}. Moreover, the affected {@link Appendable} is excluded
     * from the list of multiplexed targets, to prevent errors from piling up.
     *
     * @param target The target.
     * @param index The index of the {@link Appendable} in the array of targets.
     * @param t The error.
     * @throws IOException on error.
     */
    @Override
    protected void onError(Appendable target, int index, Throwable t) throws IOException {
      exclude(target, index);
      setThrowable(target, index, t);
    }

    /**
     * Returns the first error, or {@code null} if no error.
     *
     * @return The error (a throwable which is either a {@link IOException},
     *         {@link RuntimeException} or {@link Error}.
     */
    public Throwable getFirstError() {
      for (Throwable t : errors) {
        if (t != null) {
          return t;
        }
      }
      return null;
    }

    /**
     * Returns the first {@link Appendable} that has an error.
     *
     * @return The {@link Appendable}, or {@code null} if no error.
     * @see #getFirstErrorWithAppendable() to get both {@link Appendable} and error in one call.
     */
    public Appendable getFirstAppendableWithError() {
      int index = 0;
      for (Throwable t : errors) {
        if (t != null) {
          return targets[index];
        }
        index++;
      }
      return null;
    }

    /**
     * If there is an error, an {@link java.util.Map.Entry} of {@link Appendable} to
     * {@link Throwable} is returned. {@code null} otherwise.
     *
     * @return The {@link java.util.Map.Entry}.
     */
    public Map.Entry<Appendable, Throwable> getFirstErrorWithAppendable() {
      int index = 0;
      for (Throwable t : errors) {
        if (t != null) {
          return new AbstractMap.SimpleEntry<>(targets[index], t);
        }
        index++;
      }
      return null;
    }

    /**
     * Returns the first error that occurred.
     *
     * @return The {@link Appendable}, or {@code null} if no error.
     * @see #getFirstErrorWithAppendable() to get both {@link Appendable} and error in one call.
     */
    public Throwable getError(Appendable target) {
      int index = 0;
      for (Appendable app : targets) {
        if (app == target && errors[index] != null) { // NOPMD
          return errors[index];
        }
        index++;
      }
      return null;
    }

    /**
     * Checks if the given {@link Appendable} has an error.
     *
     * @param target The {@link Appendable}.
     * @return {@code true} if an error occurred.
     */
    public boolean hasError(Appendable target) {
      return getError(target) != null;
    }

    /**
     * Checks if any error occurred.
     *
     * @return {@code true} if an error occurred.
     */
    public boolean hasError() {
      return getFirstError() != null;
    }

    /**
     * Checks if any {@link Appendable} has an error, and throws the error (which either is an
     * {@link IOException}, {@link RuntimeException} or {@link Error}).
     *
     * @throws IOException on error.
     */
    public void checkError() throws IOException {
      Throwable t = getFirstError();
      if (t != null) {
        throwError(t);
      }
    }

    /**
     * Checks if the given {@link Appendable} has an error, and throws the error (which either is an
     * {@link IOException}, {@link RuntimeException} or {@link Error}).
     *
     * @throws IOException on error.
     */
    public void checkError(Appendable target) throws IOException {
      if (target != null) {
        Throwable t = getError(target);
        if (t != null) {
          throwError(t);
        }
      }
    }
  }

  private boolean isEnabled(Appendable target, int index) {
    if (target == null) {
      excluded.set(index);
      return false;
    } else {
      return !excluded.get(index);
    }
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    int index = 0;
    for (Appendable target : targets) {
      if (isEnabled(target, index)) {
        try {
          target.append(csq);
        } catch (IOException | RuntimeException | Error e) {
          onError(target, index, e);
        }
      }
      index++;
    }

    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    int index = 0;
    for (Appendable target : targets) {
      if (isEnabled(target, index)) {
        try {
          target.append(csq, start, end);
        } catch (IOException | RuntimeException | Error e) {
          onError(target, index, e);
        }
      }
      index++;
    }

    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    int index = 0;
    for (Appendable target : targets) {
      if (isEnabled(target, index)) {
        try {
          target.append(c);
        } catch (IOException | RuntimeException | Error e) {
          onError(target, index, e);
        }
      }
      index++;
    }

    return this;
  }

  /**
   * Marks the given {@link Appendable} as excluded.
   *
   * @param target The target.
   * @param index The index of the {@link Appendable} in the array of targets.
   */
  protected final void exclude(Appendable target, int index) {
    if (targets[index] == target) { // NOPMD
      excluded.set(index);
    }
  }

  private static void throwError(Throwable t) throws IOException {
    if (t instanceof IOException) {
      throw (IOException) t;
    } else if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else if (t instanceof Error) {
      throw (Error) t;
    } else {
      throw new IllegalStateException(t);
    }
  }

  /**
   * Called upon encountering an error (an {@link IOException}, {@link RuntimeException} or
   * {@link Error}) during {@link #append(char)}, {@link #append(CharSequence)} or
   * {@link #append(CharSequence, int, int)}.
   *
   * By default, the error is thrown.
   *
   * @param target The target.
   * @param index The index of the {@link Appendable} in the array of targets.
   * @param t The error.
   * @throws IOException on error.
   */
  protected void onError(Appendable target, int index, Throwable t) throws IOException {
    throwError(t);
  }
}
