package com.github.ayz6uem.restdoc.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ayz6uem.restdoc.RestDoc;
import com.github.ayz6uem.restdoc.http.HttpMessage;
import com.github.ayz6uem.restdoc.http.HttpRequest;
import com.github.ayz6uem.restdoc.http.HttpResponse;
import com.github.ayz6uem.restdoc.schema.Cell;
import com.github.ayz6uem.restdoc.schema.Group;
import com.github.ayz6uem.restdoc.schema.Node;
import com.github.ayz6uem.restdoc.schema.Tree;
import com.github.ayz6uem.restdoc.util.AttributeAsciidocBuilder;
import com.github.ayz6uem.restdoc.util.ObjectMappers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class AsciidocHandler implements RestDocHandler {

    AttributeAsciidocBuilder builder = AttributeAsciidocBuilder.newInstance();

    @Override
    public void handle(RestDoc restDoc) {
        Tree tree = restDoc.getTree();
        builder.documentTitle(tree.getName());
        if(Objects.nonNull(tree.getDescription())){
            builder.paragraph(tree.getDescription(),true);
        }

        for (int i = 0; i < tree.getNodes().size(); i++) {
            Node node = tree.getNodes().get(i);
            buildNode(node, "", i + 1);
        }

        String adoc = restDoc.getEnv().getOut() + "/" + restDoc.getEnv().getProject();

        Path indexFile = Paths.get(adoc);
        builder.writeToFile(indexFile, StandardCharsets.UTF_8);
    }


    private void buildNode(Node node, String prefix, int num) {
        if (node instanceof Group) {
            buildGroup((Group) node, prefix, num);
        } else if (node instanceof HttpMessage) {
            buildHttpMessage((HttpMessage) node, prefix, num);
        }
    }

    private void buildGroup(Group group, String prefix, int num) {
        builder.sectionTitleLevel1(prefix + num + " " + group.getName());
        if (Objects.nonNull(group.getDescription())) {
            builder.paragraph(group.getDescription(),true);
        }
        for (int i = 0; i < group.getNodes().size(); i++) {
            Node node = group.getNodes().get(i);
            buildNode(node, prefix + num + ".", i + 1);
        }
    }

    private void buildHttpMessage(HttpMessage message, String prefix, int num) {
        builder.sectionTitleLevel2(prefix + num + " " + message.getName());
        if (Objects.nonNull(message.getDescription())) {
            builder.paragraph(message.getDescription(),true);
        }

        HttpRequest request = message.getRequest();
        builder.block(builder -> {
            builder.textLine(request.getMethod()
                    + " "
                    + request.getUri()
                    + " "
                    + message.getVersion());
            request.getHeaders().forEach((k,v) -> builder.textLine(k+": "+v));
            builder.newLine();
            if(request.getBody()!=null){
                if(request.getBody() instanceof JsonNode){
                    builder.textLine((ObjectMappers.toPretty(request.getBody())));
                }else{
                    builder.textLine(String.valueOf(request.getBody()));
                }
            }
        }, "REQUEST");

        table(request.getCells());

        HttpResponse response = message.getResponse();
        if (!response.isEmpty()) {
            builder.block(builder -> {
                builder.textLine(message.getVersion()+" " + response.getStatus());
                response.getHeaders().forEach((k,v) -> builder.textLine(k + ": "+v));
                builder.newLine();
                if(response.getBody()!=null){
                    if(response.getBody() instanceof JsonNode){
                        builder.textLine((ObjectMappers.toPretty(response.getBody())));
                    }else{
                        builder.textLine(String.valueOf(response.getBody()));
                    }
                }
            }, "RESPONSE");
            table(response.getCells());
        }

    }

    private void table(List<Cell> cells){
        if (cells.size() > 0) {
            List<List<String>> responseTable = new ArrayList<>();
            responseTable.add(Arrays.asList("NAME", "TYPE", "DEFAULT", "DESCRIPTION"));
            cells.forEach(parameter -> responseTable.add(parameter.toList()));
            builder.table(responseTable);
        }

    }

}