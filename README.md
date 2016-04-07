# Augment Proxy
The primary goal of the AugmentProxy is to expand the features of the GSA by acting as a proxy between the GSA and the content to index. The AugmentProxy might be useful in other areas, however they are not the primary focus. It is setting up upon the plexi framework released by Google which is intended to develop adaptors for the GSA. If you are not familiar with the framework, take a look [here](http://googlegsa.github.io/adaptor/index.html).

I started this project by the end of 2015, way before Google announced that the GSA would only exist as a cloud service in the future. After this announcement in February 2016 my motivation to finish the project dropped quite a bit, so I put the project on hold for a while. I have now decided to release it anyways for several reasons: 
- it would be a shame to let all the work rot
- it might even be useful with the new cloud search
- it might be useful for other search engines as well

That being said, the project is still work in progress and might also have some rough edges. So all in all a perfectly normal project ;-).

##### General information
- works with Java 7 and higher
- currently built with Maven
- MIT License

##### Build notes
- The project uses version 1.8.4 of [jsoup](https://github.com/jhy/jsoup) which is not yet released (as of the date of writing). This is necessary because the application must be able to handle multiple equal HTTP header keys, which only jsoup 1.8.4 handles correctly. You have to build that version of jsoup yourself and put it into your Maven repository.
- The project also uses the sitemap-parser, a library to process sitemaps. You can download and build that library [here](https://github.com/bithazard/sitemap-parser).
- Lastly you need the mentioned plexi framework, which can be downloaded [here](https://github.com/googlegsa/library/releases/download/v4.1.0/adaptor-4.1.0-withlib.jar). 

#### Setup
The AugmentProxy is a program that is simply started by executing

    java -jar augment-proxy-<version>.jar
A server will then be started which listens at 2 network ports. Before you do that however, a few preparations have to be made. Also keep in mind that the adaptor might need a lot of memory. All the content that is passed through will be stored in memory to be processed at least for a short time.

##### Configuration on the GSA
Go to _Content Sources > Web Crawl > Proxy Servers_ and configure the URL pattern for which you would like to use the AugmentProxy. Configure the IP address or hostname of the server you are using for the AugmentProxy. Configure the port you are using. 

##### Configuration of the AugmentProxy
Put a file named "adaptor-config.properties" in the same folder as the jar file. This is a standard Java properties file in which you have to configure at least the "gsa.hostname" property. You might also want to configure "feed.name", "server.port" and "server.dashboardPort". Please do NOT configure "docId.isUrl" and "server.docIdPath". For a complete list of config options see https://googlegsa.github.io/librarydocsreleased/4.1.0/lib/com/google/enterprise/adaptor/Config.html

The configuration of the AugmentProxy itself is done in 2 JSON files. These files have to be named "adaptor-config.json" and "augment-config.json" and be placed in the same folder as the jar file. You can change the names (and paths) of these files by using the config options "adaptor.config" and "augment.config" respectively in the "adaptor-config.properties" file. Both configuration files are optional. However without them the AugmentProxy will do nothing and return 404 for every request.

###### adaptor-config.json
In this file you can currently only configure URLs of sitemaps that should be fed to the GSA by the AugmentProxy. The AugmentProxy will feed the URLs from the sitemap(s) along with the last modified date to the GSA. When the GSA crawls a URL from a sitemap the AugmentProxy will add meta information like change-frequency, priority, date and language (if found in the sitemap) to the document. This feature has to be enabled in the adaptor-config.json ("addMetadata"). For every sitemap that you configure a corresponding URL pattern has to be configured in the augment-config.json (more on that in the section on augment-config.json). An example of the file would look like this:

    {
      "sitemap": [
        {
          "url": "https://www.google.com/work/sitemap.xml",
          "addMetadata": true
        }
      ]
    }
As you see, you can configure multiple sitemaps, which all have to have at least the URL property. The property "addMetadata" is optional and defaults to false. The AugmentProxy feeds the complete list of URLs from the sitemap at specific times (every night at 3 AM by default). This schedule can be configured in the adaptor-config.properties with the config option "adaptor.fullListingSchedule" (see [here](https://googlegsa.github.io/librarydocsreleased/4.1.0/lib/com/google/enterprise/adaptor/Config.html)). Changed URLs from the sitemap are fed in certain intervals. By default this interval is 15 minutes. If your sitemaps change less often (which they probably do), you should increase this interval. You can do this using the config option "adaptor.incrementalPollPeriodSecs" (also in the adaptor-config.properties). 

###### augment-config.json
This is the central config file for the AugmentProxy. The concept is that you have to configure a URL pattern for every URL that you want the AugmentProxy to handle. If the AugmentProxy does not find a URL pattern that matches a request, it will return 404 (this also applies to URLs from sitemaps that you configured). The second important concept to understand is, that the order of the URL patterns is important. The AugmentProxy will process them from top to bottom. This means that you can write URL patterns in a way that a document would match multiple patterns. In this case only the first (meaning topmost) URL pattern will apply. This makes it possible to specify very specific patterns at the top and more general patterns at the bottom to catch these documents that did not match the specific patterns. 

Configurations for a URL pattern can be quite long with many options configured or just the URL pattern itself. If you configure just the URL pattern itself, the AugmentProxy will simply pass through these documents without making any changes. The following table lists all properties that can be configured for a URL pattern:

| Config option       | Type             | Default                                                             | Description                                                                                                                                                                                                                                                                                                                        |
|---------------------|------------------|---------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| setNoFollow         | boolean          | false                                                               | Always set the nofollow HTTP header for all documents from this URL pattern.                                                                                                                                                                                                                                                       |
| setCrawlOnce        | boolean          | false                                                               | Always set the crawl_once HTTP header for all documents from this URL pattern.                                                                                                                                                                                                                                                     |
| setNoArchive        | boolean          | false                                                               | Always set the noarchive HTTP header for all documents from this URL pattern.                                                                                                                                                                                                                                                      |
| setNoIndex          | boolean          | false                                                               | Always set the noindex HTTP header for all documents from this URL pattern.                                                                                                                                                                                                                                                        |
| sortParameters      | boolean          | false                                                               | Sort the URL parameters of all links (\<a href="..."/\>) that are contained in documents from this URL pattern. Useful to eliminate duplicated documents that only differ in the order of the URL parameters.                                                                                                                      |
| passGsaHeaders      | boolean          | false                                                               | Pass the GSA headers for documents from this URL pattern when the AugmentProxy is used to augment another plexi adaptor. Necessary when these specific information in the HTTP headers should not be lost.                                                                                                                         |
| executeJavascript   | boolean          | false                                                               | Execute Javascript that is contained in the documents from this URL pattern. This includes loading additional resources via Ajax. The library [htmlunit](http://htmlunit.sourceforge.net/) is used for this purpose. Note that this might use a lot of resources depending on the Javascript on the page.                          |
| javascriptTimeout   | int              | 10000                                                               | Only relevant when executeJavascript is enabled. The time in milliseconds that the AugmentProxy waits for the execution of Javascript to finish for documents from this URL pattern.                                                                                                                                               |
| userAgent           | string           | "Mozilla/5.0 (Windows NT 6.1; rv:38.0) Gecko/20100101 Firefox/38.0" | The user-agent string that is used when requesting documents from this URL pattern.                                                                                                                                                                                                                                                |
| omitContentRegex    | string           | null                                                                | A regex that is matched against the HTML content (ignored for non-HTML content) of documents from this URL pattern. When the pattern matches, the AugmentProxy returns 404 for this document. Note that you probably want to prefix your regex with "(?s)" to match newlines with a ".".                                           |
| excludeCssSelectors | array of strings | empty array                                                         | One or multiple CSS selector(s) that is/are run against documents from this URL pattern. If a CSS selector returns one/multiple node(s), this/these node(s) is/are wrapped in an "googleoff: index" and "googleon: snippet" HTML comment which causes the GSA to ignore this content (it will still follow links in this content). |
| removeParameters    | array of strings | empty array                                                         | One or multiple regex pattern(s) that is/are matched against all parameters from links (\<a href="..."/\>) that are contained in documents from this URL pattern. If a parameter matches the pattern, this parameter is removed. Useful if a parameter is not required for a URL but leads to duplicate content on the GSA.        |
| headersToMetadata   | object           | null                                                                | A simple map with a string key and a string value, mapping an HTTP header name to a metadata name. This map is used to add the HTTP headers with the respective names (from the response of a requested URL) to metadata for documents from this URL pattern.                                                                      |
| addRequestHeaders   | object           | null                                                                | A simple map with a string key and a string value, that is used to add additional HTTP headers to requests for documents from this URL pattern.                                                                                                                                                                                    |
| imageLinksToPdf     | object           | null                                                                | Explained in the next section.                                                                                                                                                                                                                                                                                                     |
| extract             | object           | null                                                                | Explained in the next section.                                                                                                                                                                                                                                                                                                     |

The imageLinksToPdf property can be used to create a very simplistic image search. When configured the AugmentProxy processes all image tags and adds the URLs of the images as new links to the document. These new links are surrounded by googleoff comments for index and snippet to not modify the actual document. When enabled ("addSurroundingText") the AugmentProxy also tries to find surrounding text around the image and combines that with the link it creates. Images can then be found with this text. The "alt" attribute of an image is always added as text to the link. Furthermore certain attributes of the image (width, height, color depth, ...) are added as metadata. When the GSA crawls such an image (you have to allow the respective file ending on the GSA of course), the AugmentProxy will not return an image but a PDF file (with content-type PDF). This allows the GSA to generate document previews for the image. The PDF contains only one page which has the size of the image. URLs of those PDFs always contain the URL parameter "convert2pdf=true". The displayUrl is the URL of the actual image.

| Config option            | Type    | Default | Description                                                                                                                                                                                 |
|--------------------------|---------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| minWidth                 | int     | 0       | The minimum width an image must have to be considered.                                                                                                                                      |
| minHeight                | int     | 0       | The minimum height an image must have to be considered.                                                                                                                                     |
| addSurroundingText       | boolean | false   | Tries to find surrounding text around the image and combines it with the link it generates so that the image can be found with this text. This might not always find the intended text.     |
| surroundingTextMinLength | int     | 5       | The minimum length the surrounding text must have. When the text that was found is shorter, the search will be continued until the surrounding text has a least the configured length.      |
| surroundingTextMaxLength | int     | 500     | The maximum length the surrounding text may have. When the text that was found is longer, no surrounding text is used for that image (except the text from a potential "alt" attribute).    |

The extract property is the most complex but also the most powerful property. In short you can run custom regexes and/or CSS selectors in certain scopes to extract specific content from a document. This content can then be added as metadata, title, link or ACL to the document. The many combinations that are possible, make this feature extremely powerful. You can of course define multiple extract rules for a URL pattern and all of them are applied to each document.

| Config option   | Type             | Default        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------|------------------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| scope           | string           | n/a - required | Must be present. Possible values are "content" and "url". Defines on which scope the regex and/or the CSS selector should be applied. In case of "content" the configured "cssSelector" is first applied to the content of the document. After this the configured "regexFind" as well as the "regexReplace" are applied to each element that was returned by the CSS selector (can be multiple). When scope is "url", a CSS selector can of course not be applied on the URL of the document. If a CSS selector is configured anyway, it is ignored and a warning is logged. Only  the configured"regexFind" as well as the "regexReplace" are applied to the URL of the document.                                                                                               |
| target          | string           | n/a - required | Must be present. Possible values are "metadata", "title", "link", "acl_user" and "acl_group". In case of "metadata", the extracted string(s) is/are added as metadata to the document. The name(s) of the metadata can be configured with "metadataNames". For "title" the extracted string is set as title (HTML tag in head section) of the document. If a title already exists, it is overwritten. If multiple strings are extracted, which string to use, can be configured with "titleCssElement". For "link" the extracted strings are added as links to the document. The links are added as HTTP headers and not to the document itself. In case of "acl_user" or "acl_group" the extracted string are added as allow users or allow groups respectively to the document. |
| cssSelector     | string           | null           | The CSS selector to run against the document before applying the regexes. Only valid when "scope" is "content", ignored otherwise. The CSS selector can return multiple elements. When no CSS selector is configured, the whole document is considered.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| regexMatch      | string           | "text"         | Possible values are "text" and "html". Defines against what the configured "regexFind" should be matched. In case of "text" the regex is only matched against the text content (HTML tags are ignored). In case of "html" the regex is matched against the full HTML content. The value "html" is of course only valid when "scope" is "content" and is ignored otherwise.                                                                                                                                                                                                                                                                                                                                                                                                        |
| regexFind       | string           | ".*"           | The regex that should be matched against the configured scope. If the regex matches or matches partly, the matching part of this regex is used as value for whichever target you configured unless "regexReplace" is also configured. In this case the string constructed there is used as value. You can define capturing groups here which can then be used in "regexReplace". Note that "regexMatch" configures whether the regex is matched against the HTML or text of a document (only when "scope" is "content").                                                                                                                                                                                                                                                          |
| regexReplace    | string           | null           | The string that is used as final value for whichever target you configured if "regexFind" matches or matches partly. You can use capturing group(s) from "regexFind" here. Note that capturing groups in Java are written like this: "$1" - this would be the first capturing group. If "regexReplace" is null, the (matching) result from "regexFind" is used as final value.                                                                                                                                                                                                                                                                                                                                                                                                    |
| metadataNames   | array of strings | empty array    | The names that should be used for the extracted metadata. Must contain at least one entry when "target" is "metadata". If "target" is something other than "metadata" configured entries are ignored. When "scope" is "url" you can only extract at most one string. Consequently when more than one metadata name is configured here, these are also ignored. If more elements are returned than metadata names configured, the additional elements are discarded. If your CSS selector returns an element that you don't need as metadata, you can configure an "_" as metadata name to skip it.                                                                                                                                                                                |
| titleCssElement | int              | 0              | The index of the matched CSS element that should be used as title of the document. Note: this setting is zero-based. So 0 will get you the first element, 1 the second and so on. When "scope" is "url" you can only extract at most one string. Consequently when "titleCssElement" is set to a number larger than 0, 0 is used.                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

###### Example config

    [
      {
        "urlPattern": "http://www.example.com/.*",
        "setNoFollow": true,
        "setCrawlOnce": true,
        "setNoArchive": true,
        "setNoIndex": true,
        "sortParameters": true,
        "passGsaHeaders": true,
        "executeJavascript": true,
        "javascriptTimeout": 30000,
        "userAgent": "gsa-crawler",
        "omitContentRegex": "(?s).*You don't have permission to access.*",
        "excludeCssSelectors": [
          "div.header",
          "div.footer",
          "#navigation"
        ],
        "removeParameters": [
          "p_auth.*"
        ],
        "headersToMetadata": {
          "Content-Length": "size",
          "Last-Modified": "changed"
        },
        "addRequestHeaders": {
          "Proxy-Authorization": "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        },
        "imageLinksToPdf": {
          "minWidth": 300,
          "minHeight": 300,
          "addSurroundingText": true,
          "surroundingTextMinLength": 10,
          "surroundingTextMaxLength": 400,
        },
        "extract": [
          {
            "scope": "content",
            "target": "metadata",
            "cssSelector": "div.main > span.date",
            "regexFind": "([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{4}|[0-9]{2})",
            "regexMatch": "html",
            "regexReplace": "$3-$2-$1",
            "metadataNames": [
              "publishing-date"
            ]
          },
          {
            "scope": "content",
            "target": "title",
            "cssSelector": "div.main > h1"
          }
        ]
      },
      {
        "urlPattern": ".*/robots.txt"
      }
    ]
