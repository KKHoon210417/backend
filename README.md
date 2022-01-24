# YUM-YUM-TREND (얌얌 트렌드)

## 🕸 시스템 구성도

![image](https://user-images.githubusercontent.com/82690689/150101613-9570aa4a-7020-4886-a5dd-b1f36cecf7c0.png)

<br>

## 🔗 라이브
<a href="https://helpmymenu.site">https://www.yumyumtrend.site</a>

<br>

## 📢 소개
여러분만의 맛집 리스트와 하루 동안 먹은 음식들을 기록하고 친구들과 공유해보세요.

![image](https://user-images.githubusercontent.com/90819869/145738907-1b0dd6f0-083b-436e-a4b5-3fa1acc197c6.png)

<br>

## 🗓 개발기간
**[3차]** 2021년 11월 19일 ~ 2021년 12월 09일 (21일)

<br>

## 🧙 멤버구성
### Back-End & Front-End

[:octocat:](https://github.com/hellonayeon) 권나연

[:octocat:](https://github.com/KKHoon210417) 김광훈 

[:octocat:](https://github.com/HWON0720) 안휘원 

[:octocat:](https://github.com/profoundsea25) 양해준

<br>


## 🛠 사용기술 &nbsp;

### 프론트엔드

`HTML` / `Javascript` / `CSS`

`Kakao SDK` `Google Geolocation` `Kakao Local`

### 백엔드

`Spring Boot` `Spring Security` `Spring Data JPA` `Spring Cloud` `Rest API Docs` `MySQL`

`Python`: `requests`


#### 인프라

`Cloud Front` `S3`

`Route 53` `Elastic Beanstalk` `RDS` `Secret Manager` `ACM`

<br>

## 💡 나의 사용기술 &nbsp;

- nGrinder를 이용해 게시글 호출 API 성능 테스트를 구현했습니다.
- Spring Data JPA의 Pageable을 사용해서 infinite Scroll을 구현했습니다.
- Git을 이용해 브랜치와 Issue를 관리했습니다.
- 'mockmvc'를 이용해 E2E 테스트를 구현했습니다.
- Spring Rest Docs를 사용해서 프로젝트 API 문서 자동화를 구현했습니다.
- Geolcation API와 Kakao Local API를 사용해서 위치정보 검색 기능을 구현했습니다.
- Spring Boot를 사용해서 API를 Restful하게 구현했습니다.
- Spring Data JPA를 사용해서 객체지향적으로 CRUD 객체를 구현했습니다.

<br>

## 💻 핵심 기능 &nbsp;

### 트렌드 구현을 위한 '위치데이터' 저장 기능

트렌드 기능을 구현하기 위해 Kakao Open API와 Google Geolocation API를 이용해 위치 데이터를 수집하여 트렌드 기능을 구현했습니다.

<br>

<details markdown="10">
<summary>Google Geolocation API 기반 GPS기능 구현</summary>

   - Google Geolocation API를 사용해서 사용자의 좌표를 확인 & 저장하였습니다.
   
   ``` javascript
   function getCoordinate() {
    $.ajax({
        type: "POST",
        url: `https://www.googleapis.com/geolocation/v1/geolocate?key=${GOOGLE_GEOLOCATION_API_KEY}`,
        data: {},
        success: function (response) {
            let location = response.location;
            gLat = location.lat;
            gLng = location.lng;
            console.log(gLat, gLng);
        }
    })
}
   ```
   
</details>

<details markdown="11">
<summary>Kakao Local API 기반 위치 데이터 수집 기능 구현</summary>

   - Kakao Local API를 이용해서 사용자의 위치 데이터를 수집했습니다.
   - 위치 데이터는 다음과 같은 항목을 수집합니다.
     - 도로명 주소
     - 위치명
     - 좌표
     - 카테고리
   - 검색 데이터가 많은 관계로 페이징을 추가하여 한정된 공간 안에 위치데이터를 표시하도록 설정하였습니다.
   
   ``` javascript
   function getLocation(currentPage) {
    deleteSelectLocation()
    $("#article-location-list-div").empty();

    console.log(currentPage)
    $.ajax({
        type: "GET",
        url: (gLat
            ? `https://dapi.kakao.com/v2/local/search/keyword.json?y=${gLat}&x=${gLng}&radius=2000&page=${currentPage}&size=${KAKAO_LOCATION_SIZE}&query=` + encodeURIComponent($("#search-input").val())
            : `https://dapi.kakao.com/v2/local/search/keyword.json?&page=${currentPage}&size=${KAKAO_LOCATION_SIZE}&query=` + encodeURIComponent($("#search-input").val())),
        headers: {'Authorization': `KakaoAK ${KAKAO_SEARCH_API_KEY}`},
        success: function (response) {
            console.log(response)
            let tempHtml = ``
            let locationInfoList = response.documents
            let pagingInfo = response.meta
            if (parseInt(pagingInfo.total_count / KAKAO_LOCATION_SIZE) >= KAKAO_LOCATION_MAX_RESULT) {
                pagingInfo.totalPage = pagingInfo.totalPage = KAKAO_LOCATION_MAX_RESULT;
            } else {
                if (pagingInfo.total_count % KAKAO_LOCATION_SIZE == 0) {
                    pagingInfo.totalPage = parseInt(pagingInfo.total_count / KAKAO_LOCATION_SIZE);
                } else {
                    pagingInfo.totalPage = parseInt(pagingInfo.total_count / KAKAO_LOCATION_SIZE) + 1;
                }
            }
            pagingInfo.currentPage = currentPage;
            for (let i = 0; i < locationInfoList.length; i++) {
                tempHtml = addLocationList(locationInfoList[i], i + 1)
                $("#article-location-list-div").append(tempHtml);
            }
            addPaging(pagingInfo, currentPage - 1);
        },
        error: function (e) {
            console.log(e);
        }
    })
}

   function addLocationList(locationInfo, idx) {
    let roadAddressName = locationInfo.road_address_name;
    let placeName = locationInfo.place_name;
    let xCoordinate = locationInfo.x;
    let yCoordinate = locationInfo.y;
    let categoryName = locationInfo.category_name
    return `<div>
                <a href="#" class="location-list-font-size" onclick="selectLocation(${idx})">${placeName} (${roadAddressName})</a>
                <span id="location-idx-${idx}" hidden>${roadAddressName}@${placeName}@${xCoordinate}@${yCoordinate}@${categoryName}</span>
            </div>`
}
   ```
   
</details>

<details markdown="12">
<summary>데이터 전처리 & 저장</summary>

- 트렌드 데이터(Tag,Location)는 게시글 저장(Article)과 함께 데이터베이스 상에 저장됩니다.
- 위치 데이터의 카태고리 데이터는 트렌드 데이터에 사용할 수 있도록 전처리 과정을 거친 후 저장됩니다.


![image](https://user-images.githubusercontent.com/82690689/150101221-c97f0868-6841-4969-a983-869fcdb265d4.png)

```JAVA
@Transactional
    public void createArticle(User user, String text, LocationRequestDto locationRequestDto, List<String> hashtagNameList, List<MultipartFile> imageFileList) {
        LocationRequestDto locationInfo = dataPreprocessing.categoryDataPreprocessing(locationRequestDto);
        Location location = locationRepository.save(new Location(locationInfo, user.getId()));

        Article article = articleRepository.save(new Article(text, location, user));

        for(String tag : hashtagNameList) {
            hashtagRepository.save(new Hashtag(tag, article, user));
        }
        for(MultipartFile multipartFile : imageFileList) {
            String url = fileService.uploadImage(multipartFile);
            Image image = new Image(url, article);
            imageRepository.save(image);
        }
    }
}

@Component
public class DataPreprocessing {

    public LocationRequestDto categoryDataPreprocessing(LocationRequestDto locationRequestDto) {
        String categoryInfo = locationRequestDto.getCategoryName();
        String[] infoBundle = categoryInfo.split(" > ");
        ArrayList<String> infoList = new ArrayList<String>(Arrays.asList(infoBundle));

        // 전처리 완료한 카테고리 값 저장
        locationRequestDto.setCategoryName(infoList.get(infoList.size() - 1));
        return locationRequestDto;
    }
}

```
   
</details>


<br>

## 💡 핵심 트러블 슈팅 &nbsp;

### 게시글 리스트 API 속도 2배 개선
nGrinder를 기반으로 API 성능테스트를 진행했습니다.<br>
'게시글 조회' API의 성능테스트 때 40~50명의 가상 사용자 수를 설정했을 때, TPS 수치가 40 ~ 50으로 동일하거나 그 보다 높은 값이 나오기를 기대했으나, 해당 값에 미치지 못하는 결과가 나왔습니다.
- 조건
  - Vusers: 40(Process 2, Thread 20)
  - RunCount: 100
  - nGrinder Controller: EC2 t2.xlarge
  - nGrinder Agent: EC t2.xlarge
  - Application Server(Target Server): EC2 t3.large
  
<details markdown="1">
<summary>개선 전 성능테스트</summary>

![개선사항적용전(dto)](https://user-images.githubusercontent.com/82690689/150091142-e084894c-850f-4c0a-a37f-385429acdb57.png)

이를해결하기 위해 먼저 Target Server의 과부하로 인한 문제가 있는 듯해 Target Server를 두개로 늘리는 Scale Out을 실시해 보았습니다.
  
</details>

<details markdown="2">
<summary>Scale Out 후 성능테스트</summary>
   
![개선사항적용후(스케일아웃)](https://user-images.githubusercontent.com/82690689/150091558-2df821e5-d3ad-4352-bba6-51dade975824.png)

TPS가 약간 증가했지만, 의미있는 값이 증가했다고 볼 수 없다고 판단하여 코드 수정을 통해 성능 개선을 시키고자 했습니다.<br>
  
</details>

<details markdown="3">
<summary>개선 방법</summary>
- 일대다 관계로 연관된 변수들의 패치 타입을 지연 로딩으로 불러오도록 개선.

![image](https://user-images.githubusercontent.com/82690689/150092374-26346949-662c-41a8-b9cc-38d1ba9d6063.png)

- Entity 자체를 Response해주는 것이 아니라 DTO에 담아 필요 내용만 Response해주도록 개선.

![image](https://user-images.githubusercontent.com/82690689/150092392-89d32615-810f-48a8-b1f5-a4cee7e3a30b.png)
  
</details>

<details markdown="4">
<summary>결과</summary>
- 수정 전(32 + 32 쿼리)

![image](https://user-images.githubusercontent.com/82690689/150092761-ed566ec1-0379-48ed-bb77-3a30c02094e0.png)

- 수정 후(32 쿼리)

![image](https://user-images.githubusercontent.com/82690689/150092778-778d1271-cb7d-4300-b79c-14af6d3387c2.png)

32개의 게시물 조회 요청 시 지연 로딩을 사용함으로써 불필요한 32번의 쿼리 제거

- 코드 개선 후 성능테스트

![3분돌림](https://user-images.githubusercontent.com/82690689/150093147-5203fc9a-f760-4624-880c-08159acbed52.png)

TPS 값이 19.4에서 37.6으로 약 두배 상승한 것을 확인할 수 있습니다.
이를 통해 불필요한 쿼리 호출을 막는 것이 성능 개선에 많은 영향을 미친다는 것을 배울 수 있었습니다.
</details>

<br>

## 👾 그 외 트러블 슈팅 &nbsp;
 
<details markdown="5">
<summary>infinite Scroll 게시글 중복 문제</summary>
  
   - 인스타그램과 같이 사용자가 게시글을 볼 때, 스크롤을 아래로 내리면 자동으로 새로운 게시글이 출력해주기 위해 인피니티 스크롤을 적용하였습니다.
   - 인피니티 스크롤 적용 후 A사용자가 게시글을 볼 때, B사용자가 게시글을 등록하게되면 A사용자는 같은 게시글이 두번 중복되어 보이는 문제가 발생하였습니다.
   - 해당 문제가 Pagenation일 경우 사용자가 새로운 게시글이 등록되었다고 인지하고 넘어가겠지만 인피니티 스크롤의 경우 사용자에게 중복된 게시글이 두개가 보이게 됩니다.
   - 이를 해결하기 위해 사용자가 보는 마지막 게시글 id값보다 낮은 게시글 id만 호출해오도록 수정함으로서 문제를 개선하였습니다.
   
   
   <details markdown="6">
   <summary>적용 전</summary>
      
   - Controller
      
   ``` java
    @GetMapping("/articles")
    public Page<ArticleResponseDto> getArticles(@RequestParam(required = false) String searchTag,
                                                @RequestParam(required = false) String location,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String tagName,
                                                @RequestParam("sortBy") String sortBy,
                                                @RequestParam("isAsc") boolean isAsc,
                                                @RequestParam("currentPage") int page) {
        return articleService.getArticles(searchTag, location, category, tagName, sortBy, isAsc, page);
    }
   ```
      
   - Service
      
   ```java
      public Page<ArticleResponseDto> getArticles(String searchTag, String location, String category, String tagName, String sortBy, boolean isAsc, int page) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, 32, sort);

        Page<Article> articles = null;
        if (searchTag.isEmpty()) {
            if (location.isEmpty()) {
                if (category.isEmpty() && tagName.isEmpty()) {
                    articles = articleRepository.findAll(pageable);
                } else if(tagName.isEmpty()) {
                    articles = articleRepository.findAllByLocationCategoryName(pageable, category);
                } else {
                    articles = articleRepository.findAllByTagsName(tagName, pageable);
                }
            } else {
                if (category.isEmpty() && tagName.isEmpty()) {
                    articles = articleRepository.findAllByLocationRoadAddressNameStartsWith(pageable, location);
                } else if(tagName.isEmpty()) {
                    articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndLocationCategoryName(pageable, location, category);
                } else {
                    articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndTagsName(pageable, location, tagName);
                }
            }
        } else {
            articles = articleRepository.findAllByTagsName(searchTag, pageable);
        }

        return articles.map(ArticleResponseDto::new);
    }
   ```
 
   </details>

      
   <details markdown="7">
   <summary>적용 후</summary>
      
   - Controller
      
   ```java
      @GetMapping("/articles")
    public Page<ArticleResponseDto> getArticles(@RequestParam(required = false) String searchTag,
                                                @RequestParam(required = false) String location,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String tagName,
                                                @RequestParam("lastArticleId") Long lastArticleId,
                                                @RequestParam("sortBy") String sortBy,
                                                @RequestParam("isAsc") boolean isAsc,
                                                @RequestParam("currentPage") int page) {
        return articleService.getArticles(searchTag, location, category, tagName, sortBy, isAsc, page, lastArticleId);
    }
   ```
      
   - Service
      
   ```java
      public Page<ArticleResponseDto> getArticles(String searchTag, String location, String category, String tagName, String sortBy, boolean isAsc, int page, Long lastArticleId) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, 32, sort);

        Page<Article> articles = null;
        if (lastArticleId.equals(0L)) {
            if (searchTag.isEmpty()) {
                if (location.isEmpty()) {
                    if (category.isEmpty() && tagName.isEmpty()) {
                        articles = articleRepository.findAll(pageable);
                    } else if(tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationCategoryName(pageable, category);
                    } else {
                        articles = articleRepository.findAllByTagsName(tagName, pageable);
                    }
                } else {
                    if (category.isEmpty() && tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWith(pageable, location);
                    } else if(tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndLocationCategoryName(pageable, location, category);
                    } else {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndTagsName(pageable, location, tagName);
                    }
                }
            } else {
                articles = articleRepository.findAllByTagsName(searchTag, pageable);
            }
        } else {
            if (searchTag.isEmpty()) {
                if (location.isEmpty()) {
                    if (category.isEmpty() && tagName.isEmpty()) {
                        articles = articleRepository.findAllByIdLessThan(pageable, lastArticleId);
                    } else if(tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationCategoryNameAndIdLessThan(pageable, category, lastArticleId);
                    } else {
                        articles = articleRepository.findAllByTagsNameAndIdLessThan(tagName, pageable, lastArticleId);
                    }
                } else {
                    if (category.isEmpty() && tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndIdLessThan(pageable, location, lastArticleId);
                    } else if(tagName.isEmpty()) {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndLocationCategoryNameAndIdLessThan(pageable, location, category, lastArticleId);
                    } else {
                        articles = articleRepository.findAllByLocationRoadAddressNameStartsWithAndTagsNameAndIdLessThan(pageable, location, tagName, lastArticleId);
                    }
                }
            } else {
                articles = articleRepository.findAllByTagsNameAndIdLessThan(searchTag, pageable, lastArticleId);
            }
        }



        return articles.map(ArticleResponseDto::new);
    }
   ```

   </details>
  
</details>
 
<details markdown="8">
<summary>위치 정보 등록 시 정확성 문제</summary>

   - KaKao Local을 사용했을 때(반환되는 위치 정보가 한정적), 사용자가 위치정보를 등록할 경우 위치 정보에 대한 정확성을 어떻게 높일 수 있을까에 대한 고민을 했습니다.
   - 이에 대한 해결방법으로 사용자 위치 기반으로 위치를 등록할 수 있는 기능을 추가하였습니다.
   - Google Geolcation API를 이용해서 사용자의 위치 정보(좌표)를 받으면 해당 좌표를 Kakao Local API Query값에 추가하여 보냄으로서 사용자 주변 2km반경의 위치에 대해서 검색할 수 있도록 적용하였습니다.
   
   [issue10](https://github.com/yum-yum-trend/frontend/issues/10)
   
</details>
      
<details markdown="9">
<summary>트렌드 기능 개선</summary>

   - 임의의 사용자들로부터 트렌드 기능의 유용성에 대한 피드백을 받았고 이를 해결하였습니다.
   - 가장 많은 피드백 중 하나는 차트를 클릭했을 경우 차트에 대한 게시글을 따로 보고싶다는 피드백이 중심이였습니다.
   
   [issue82](https://github.com/yum-yum-trend/backend/issues/82)
   
   
</details>
