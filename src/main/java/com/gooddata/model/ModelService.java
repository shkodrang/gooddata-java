/*
 * Copyright (C) 2007-2014, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.model;

import com.gooddata.AbstractService;
import com.gooddata.FutureResult;
import com.gooddata.GoodDataRestException;
import com.gooddata.PollHandler;
import com.gooddata.gdc.AsyncTask;
import com.gooddata.project.Project;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.Reader;

import static com.gooddata.Validate.notEmpty;
import static com.gooddata.Validate.notNull;

/**
 * Service for manipulating with project model
 */
public class ModelService extends AbstractService {

    public ModelService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    public FutureResult<ModelDiff> getProjectModelDiff(Project project, DiffRequest diffRequest) {
        notNull(project, "project");
        notNull(diffRequest, "diffRequest");
        try {
            final AsyncTask asyncTask = restTemplate
                    .postForObject(DiffRequest.URI, diffRequest, AsyncTask.class, project.getId());
            return new FutureResult<>(this, new PollHandler<ModelDiff,ModelDiff>(asyncTask.getUri(), ModelDiff.class));
        } catch (GoodDataRestException | RestClientException e) {
            throw new ModelException("Unable to get project model diff", e);
        }
    }

    public FutureResult<ModelDiff> getProjectModelDiff(Project project, String targetModel) {
        notNull(project, "project");
        notNull(targetModel, "targetModel");
        return getProjectModelDiff(project, new DiffRequest(targetModel));
    }

    public FutureResult<ModelDiff> getProjectModelDiff(Project project, Reader targetModel) {
        notNull(project, "project");
        notNull(targetModel, "targetModel");
        try {
            return getProjectModelDiff(project, FileCopyUtils.copyToString(targetModel));
        } catch (IOException e) {
            throw new ModelException("Can't read target model", e);
        }
    }

    public void updateProjectModel(Project project, ModelDiff projectModelDiff) {
        notNull(project, "project");
        notNull(projectModelDiff, "projectModelDiff");

        for (String maql : projectModelDiff.getUpdateMaql()) {
            updateProjectModel(project, maql);
        }
    }

    public FutureResult<Void> updateProjectModel(Project project, String maqlDdl) {
        notNull(project, "project");
        notEmpty(maqlDdl, "maqlDdl");
        try {
            final MaqlDdlLinks linkEntries = restTemplate.postForObject(MaqlDdl.URI, new MaqlDdl(maqlDdl),
                    MaqlDdlLinks.class, project.getId());
            return new FutureResult<>(this, new PollHandler<Void,Void>(linkEntries.getStatusLink(), Void.class) {
                @Override
                public boolean isFinished(final ClientHttpResponse response) throws IOException {
                    final boolean finished = super.isFinished(response);
                    if (finished) {
                        final MaqlDdlTaskStatus maqlDdlTaskStatus = extractData(response, MaqlDdlTaskStatus.class);
                        if (!maqlDdlTaskStatus.isSuccess()) {
                            throw new ModelException(
                                    "Unable to update project model: " + maqlDdlTaskStatus.getMessages());
                        }
                    }
                    return finished;
                }
            });
        } catch (GoodDataRestException | RestClientException e) {
            throw new ModelException("Unable to update project model", e);
        }
    }

}
