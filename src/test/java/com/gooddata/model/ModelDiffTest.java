/*
 * Copyright (C) 2007-2014, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.model;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Collections;

import static com.gooddata.model.ModelDiff.UpdateScript;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class ModelDiffTest {

    @Test
    public void testGetUpdateMaqls() throws Exception {
        final ModelDiff diff = new ModelDiff(singletonList(new UpdateScript(asList("maql1", "maql2"), true, false)));

        assertThat(diff.getUpdateMaql(), hasSize(2));
        assertThat(diff.getUpdateMaql(), contains("maql1", "maql2"));
    }

    @Test
    public void testGetUpdateMaqlsReturnsBest() throws Exception {
        final ModelDiff diff = new ModelDiff(asList(
                new UpdateScript(asList("maql1"), false, false),
                new UpdateScript(asList("maql2"), true, false))
        );

        assertThat(diff.getUpdateMaql(), hasSize(1));
        assertThat(diff.getUpdateMaql(), contains("maql2"));
    }

    @Test
    public void testGetUpdateMaqlsReturnsNotWorst() throws Exception {
        final ModelDiff diff = new ModelDiff(asList(
                new UpdateScript(asList("maql1"), false, true),
                new UpdateScript(asList("maql2"), false, false))
        );

        assertThat(diff.getUpdateMaql(), hasSize(1));
        assertThat(diff.getUpdateMaql(), contains("maql2"));
    }

    @Test
    public void testGetUpdateMaqlsNoPreserveData() throws Exception {
        final ModelDiff diff = new ModelDiff(singletonList(new UpdateScript(asList("maql"), false, false)));

        assertThat(diff.getUpdateMaql(), hasSize(1));
        assertThat(diff.getUpdateMaql().get(0), equalTo("maql"));
    }

    @Test
    public void testGetUpdateMaqlsNoUpdateScript() throws Exception {
        final ModelDiff diff = new ModelDiff(Collections.<UpdateScript>emptyList());

        assertThat(diff.getUpdateMaql(), hasSize(0));
    }

    @Test
    public void testGetUpdateMaqlsNoMaqlInUpdateScript() throws Exception {
        final ModelDiff diff = new ModelDiff(asList(new UpdateScript(Collections.<String>emptyList(), true, false)));

        assertThat(diff.getUpdateMaql(), hasSize(0));
    }

    @Test
    public void testDeserialization() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("/model/modelDiff.json");
        final ModelDiff diff = new ObjectMapper().readValue(stream, ModelDiff.class);

        assertThat(diff, is(notNullValue()));
        assertThat(diff.getUpdateScripts(), hasSize(2));
        assertThat(diff.getUpdateScripts().get(0).isPreserveData(), is(true));
        assertThat(diff.getUpdateScripts().get(0).isCascadeDrops(), is(false));
        assertThat(diff.getUpdateScripts().get(0).getMaqlChunks(), hasSize(1));
        assertThat(diff.getUpdateScripts().get(0).getMaqlChunks(), contains(
                "CREATE FOLDER {ffld.employee} VISUAL(TITLE \"Employee\") TYPE FACT;\nCREATE FACT {fact.employee.age} VISUAL(TITLE \"Employee Age\", FOLDER {ffld.employee}) AS {f_employee.f_age};\nALTER DATASET {dataset.employee} ADD {fact.employee.age};\nSYNCHRONIZE {dataset.employee} PRESERVE DATA;"));
    }
}