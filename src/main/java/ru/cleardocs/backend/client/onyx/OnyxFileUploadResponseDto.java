package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxFileUploadResponseDto(
    @JsonProperty("file_paths") List<String> filePaths,
    @JsonProperty("file_names") List<String> fileNames,
    @JsonProperty("zip_metadata_file_id") String zipMetadataFileId
) {
  public List<String> filePaths() {
    return filePaths != null ? filePaths : Collections.emptyList();
  }

  public List<String> fileNames() {
    return fileNames != null ? fileNames : Collections.emptyList();
  }
}
