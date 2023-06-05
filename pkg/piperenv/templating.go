package piperenv

import (
	"bytes"
	"fmt"
	"strings"
	"text/template"
)

const DEFAULT_START_DELIMITER = "{{"
const DEFAULT_END_DELIMITER = "}}"

// ParseTemplate allows to parse a template which contains references to the CPE
// Utility functions make it simple to access specific parts of the CPE
func (c *CPEMap) ParseTemplate(cpeTemplate string) (*bytes.Buffer, error) {
	return c.ParseTemplateWithDelimiter(cpeTemplate, DEFAULT_START_DELIMITER, DEFAULT_END_DELIMITER)
}

func (c *CPEMap) ParseTemplateWithDelimiter(cpeTemplate string, startDelimiter string, endDelimiter string) (*bytes.Buffer, error) {
	funcMap := template.FuncMap{
		"cpe":         c.cpe,
		"cpecustom":   c.custom,
		"git":         c.git,
		"imageDigest": c.imageDigest,
		"imageTag":    c.imageTag,

		// ToDo: add template function for artifacts
		// This requires alignment on artifact handling before, though
	}

	tmpl, err := template.New("cpetemplate").Delims(startDelimiter, endDelimiter).Funcs(funcMap).Parse(cpeTemplate)
	if err != nil {
		return nil, fmt.Errorf("failed to parse cpe template '%v': %w", cpeTemplate, err)
	}

	tmplParams := struct {
		CPE map[string]interface{}
	}{
		CPE: map[string]interface{}(*c),
	}

	var generated bytes.Buffer
	err = tmpl.Execute(&generated, tmplParams)
	if err != nil {
		return nil, fmt.Errorf("failed to execute cpe template '%v': %w", cpeTemplate, err)
	}

	return &generated, nil
}

func (c *CPEMap) cpe(element string) string {
	// ToDo: perform validity checks to allow only selected fields for now?
	// This would allow a stable contract and could perform conversions in case a contract changes.

	return fmt.Sprint(map[string]interface{}(*c)[element])
}

func (c *CPEMap) custom(element string) string {
	return fmt.Sprint(map[string]interface{}(*c)[fmt.Sprintf("custom/%v", element)])
}

func (c *CPEMap) git(element string) string {
	var el string
	if element == "organization" || element == "repository" {
		el = fmt.Sprint(map[string]interface{}(*c)[fmt.Sprintf("github/%v", element)])
	} else {
		el = fmt.Sprint(map[string]interface{}(*c)[fmt.Sprintf("git/%v", element)])
	}
	return el
}

func (c *CPEMap) imageDigest(imageName string) string {
	digests, _ := map[string]interface{}(*c)["container/imageDigests"].([]interface{})
	imageNames, _ := map[string]interface{}(*c)["container/imageNames"].([]interface{})
	if len(digests) > 0 && len(digests) == len(imageNames) {
		for i, image := range imageNames {
			if fmt.Sprint(image) == imageName {
				return fmt.Sprint(digests[i])
			}
		}
	}
	return ""
}

func (c *CPEMap) imageTag(imageName string) string {
	nameTags, _ := map[string]interface{}(*c)["container/imageNameTags"].([]interface{})
	for _, nameTag := range nameTags {
		nt := strings.Split(fmt.Sprint(nameTag), ":")
		if nt[0] == imageName {
			return nt[1]
		}
	}
	return ""
}