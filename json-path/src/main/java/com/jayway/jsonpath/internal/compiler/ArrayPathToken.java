package com.jayway.jsonpath.internal.compiler;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.String.format;

/**
 *
 */
public class ArrayPathToken extends PathToken {

    private static final Logger logger = LoggerFactory.getLogger(ArrayPathToken.class);

    public static enum Operation {
        CONTEXT_SIZE,
        SLICE_TO,
        SLICE_FROM,
        SLICE_BETWEEN,
        INDEX_SEQUENCE,
        SINGLE_INDEX;
    }

    private final List<Integer> criteria;
    private final Operation operation;
    private final boolean isDefinite;

    public ArrayPathToken(List<Integer> criteria, Operation operation) {
        this.criteria = criteria;
        this.operation = operation;
        this.isDefinite = (Operation.SINGLE_INDEX == operation || Operation.CONTEXT_SIZE == operation);
    }

    @Override
    public void evaluate(String currentPath, Object model, EvaluationContextImpl ctx) {
        if(model == null){
            throw new PathNotFoundException("The path " + currentPath + " is null");
        }
        if (!ctx.jsonProvider().isArray(model)) {
            throw new InvalidPathException(format("Filter: %s can only be applied to arrays. Current context is: %s", toString(), model));
        }

        try {
            int idx;
            int input;
            int length;
            int from;
            int to;

            switch (operation){
                case SINGLE_INDEX:
                    handleArrayIndex(criteria.get(0), currentPath, model, ctx);
                    break;

                case INDEX_SEQUENCE:
                    for (Integer i : criteria) {
                        handleArrayIndex(i, currentPath, model, ctx);
                    }
                    break;

                case CONTEXT_SIZE:
                    length = ctx.jsonProvider().length(model);
                    idx = length + criteria.get(0);
                    handleArrayIndex(idx, currentPath, model, ctx);
                    break;

                case SLICE_FROM: //[2:]
                    input = criteria.get(0);
                    length = ctx.jsonProvider().length(model);
                    from = input;
                    if (from < 0) {
                        //calculate slice start from array length
                        from = length + from;
                    }
                    from = Math.max(0, from);

                    logger.debug("Slice from index on array with length: {}. From index: {} to: {}. Input: {}", length, from, length - 1, toString());

                    if (length == 0 || from >= length) {
                        return;
                    }
                    for (int i = from; i < length; i++) {
                        handleArrayIndex(i, currentPath, model, ctx);
                    }
                    break;

                case SLICE_TO : //[:2]
                    input = criteria.get(0);
                    length = ctx.jsonProvider().length(model);
                    to = input;
                    if (to < 0) {
                        //calculate slice end from array length
                        to = length + to;
                    }
                    to = Math.min(length, to);

                    logger.debug("Slice to index on array with length: {}. From index: 0 to: {}. Input: {}", length, to, toString());

                    if (length == 0) {
                        return;
                    }
                    for (int i = 0; i < to; i++) {
                        handleArrayIndex(i, currentPath, model, ctx);
                    }
                    break;

                case SLICE_BETWEEN : //[2:4]
                    from = criteria.get(0);
                    to = criteria.get(1);
                    length = ctx.jsonProvider().length(model);

                    to = Math.min(length, to);

                    if (from >= to || length == 0) {
                        return;
                    }

                    logger.debug("Slice between indexes on array with length: {}. From index: {} to: {}. Input: {}", length, from, to, toString());

                    for (int i = from; i < to; i++) {
                        handleArrayIndex(i, currentPath, model, ctx);
                    }
                    break;
                }
        } catch (IndexOutOfBoundsException e) {
            throw new PathNotFoundException("Index out of bounds when evaluating path " + currentPath);
        }
    }

    @Override
    public String getPathFragment() {
        StringBuilder sb = new StringBuilder();
        if (Operation.SINGLE_INDEX == operation || Operation.INDEX_SEQUENCE == operation) {
            sb.append("[")
                    .append(Utils.join(",", "", criteria))
                    .append("]");
        } else if (Operation.CONTEXT_SIZE == operation) {
            sb.append("[@.size()")
                    .append(criteria.get(0))
                    .append("]");
        } else if (Operation.SLICE_FROM == operation) {
            sb.append("[")
                    .append(criteria.get(0))
                    .append(":]");
        } else if (Operation.SLICE_TO == operation) {
            sb.append("[:")
                    .append(criteria.get(0))
                    .append("]");
        } else if (Operation.SLICE_BETWEEN == operation) {
            sb.append("[")
                    .append(criteria.get(0))
                    .append(":")
                    .append(criteria.get(1))
                    .append("]");
        } else
            sb.append("NOT IMPLEMENTED");

        return sb.toString();
    }

    @Override
    boolean isTokenDefinite() {
        return isDefinite;
    }
}
