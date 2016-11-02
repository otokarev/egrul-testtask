# Test project: XML archives parser

## Functional requiriments

### User-stories
#### Import data
1. Application starts with archives files destination directory
1. Application read the directory and decide what zips are not loaded yet (new)
1. Application for each new zip-file:
   1. Unzip it
   1. Retrieve XMLs
   1. For every XML:
      1. Split in `<item>` nodes
      1. For every `<item>` node:
         1. Store `<item>` node translated in JSON format in Cassandra database
         1. Update Elasticsearch database with new `<item>` JSON-encoded content
#### Data access
1. Retrieve a company information
   1. for a concrete `id` with last `modifiedAt`
   1. for a concrete `id` for all `modifiedAt` sorted by `modifiedAt`

### Formats
#### EGRUL xml format:
```
<batch>
    <item><id>qewr</id><modifiedAt>2016-11-02T12:00:00</modifiedAt><payload>...</payload></item>
    ...
</batch>
```

#### JSON format
Any suitable to store in Elasticsearch with automapping

## Technical requirements

### Cassandra
* 3 nodes deployment

### Elasticsearch
* 1 node deployment
