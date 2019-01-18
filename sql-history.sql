-- Time: 2019-01-18T02:10:01.194Z

-- Generated by TestManual
SELECT
            DOM, DOM_FIRST_HREF(DOM), TOP, LEFT, WIDTH, HEIGHT, CHAR, IMG, A, CHILD, SIBLING
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html', '*:expr(child > 20 && char > 100 && width > 800)')
            ORDER BY CHILD DESC, CHAR DESC LIMIT 50;

-- Generated by TestManual
SELECT
            DOM, DOM_FIRST_HREF(DOM), TOP, LEFT, WIDTH, HEIGHT, CHAR, IMG, A, SIBLING, DOM_TEXT(DOM)
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html', '*:expr(sibling > 20 && char > 40 && char < 100 && width > 200)')
            ORDER BY SIBLING DESC, CHAR DESC LIMIT 500;

-- Generated by TestManual
SELECT DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.welcome');
SELECT DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPrice', 0, 5);
SELECT DOM_SRC(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic img', 0, 5);
SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic a', 0, 5);
SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), 'a[href~=item]', 0, 5);

-- Generated by TestManual
SELECT
  DOM_FIRST_TEXT(DOM, 'div:iN-bOx(560,27),*:IN-BOX(560,56)') AS TITLE,
  DOM_FIRST_TEXT(DOM, '*:expr(TOP>=287 && TOP<=307 && LEFT==472 && width==560 && height>=27 && height<=54 && char>=34 && char<=41)') AS TITLE2,
  IN_BOX_FIRST_TEXT(DOM, '560x27,560x56') AS TITLE3
FROM LOAD_OUT_PAGES('https://www.mia.com/formulas.html', '*:expr(img>0 && width>200 && height>200 && sibling>30)', 1, 20)
WHERE DOM_CH(DOM) > 100;

-- Generated by TestManual
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('http://news.cnhubei.com/') WHERE SIBLING>30 AND DOM_TEXT_LENGTH(DOM) > 10 AND TOP > 300 AND TOP < 3000;
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, CHAR, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('http://news.cnhubei.com/xw/jj/201804/t4102239.shtml') WHERE SEQ > 170 AND SEQ < 400;
SELECT
  DOM_FIRST_TEXT(DOM, 'H1') AS TITLE,
  DOM_FIRST_TEXT(DOM, '.jcwsy_mini_content') AS DATE_TIME,
  DOM_FIRST_TEXT(DOM, '.content_box') AS CONTENT
FROM LOAD_OUT_PAGES('http://news.cnhubei.com/', '.news_list_box', 1, 100);

-- Generated by TestManual
SELECT
  DOM_FIRST_TEXT(DOM, 'div:iN-bOx(560,27),*:IN-BOX(560,56)') AS TITLE,
  DOM_FIRST_TEXT(DOM, '.brand') AS TITLE2,
  DOM_WIDTH(DOM_SELECT_FIRST(DOM, '.brand')) AS W,
  DOM_HEIGHT(DOM_SELECT_FIRST(DOM, '.brand')) AS H,
  IN_BOX_FIRST_TEXT(DOM, '560x27,560x56') AS TITLE3
FROM LOAD_OUT_PAGES('https://www.mia.com/formulas.html', '*:expr(img>0 && width>200 && height>200 && sibling>=40)', 1, 10)
WHERE DOM_CH(DOM) > 100;

-- Generated by TestManual
SELECT
            DOM_PARENT(DOM), DOM, DOM_FIRST_HREF(DOM), TOP, LEFT, WIDTH, HEIGHT, CHAR, IMG, A, SIBLING, DOM_TEXT(DOM)
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html', '*:expr(sibling > 20 && char > 40 && char < 100 && width > 200)')
            ORDER BY SIBLING DESC, CHAR DESC LIMIT 50;

-- Generated by TestManual
CALL SET_PAGE_EXPIRES('1s', 1);
SELECT DOM, DOM_TEXT(DOM) FROM LOAD_OUT_PAGES('https://www.mia.com/formulas.html', '*:expr(width > 240 && width < 250 && height > 360 && height < 370)', 0, 20);

-- Generated by TestManual
CALL DOM_LOAD('https://www.mia.com/formulas.html');

-- Generated by TestManual
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html') WHERE SIBLING>30 AND DOM_TEXT_LENGTH(DOM) > 10 AND TOP > 300 AND TOP < 3000;
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, CHAR, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('https://www.mia.com/item-1687128.html') WHERE SEQ > 170 AND SEQ < 400;
SELECT
  DOM_FIRST_TEXT(DOM, 'div:iN-bOx(560,27),*:IN-BOX(560,56)') AS TITLE,
  IN_BOX_FIRST_TEXT(DOM, '560x27,560x56') AS TITLE3
FROM LOAD_OUT_PAGES('https://www.mia.com/formulas.html', '*:expr(img>0 && width>200 && height>200 && sibling>30)', 1, 20)
WHERE DOM_CH(DOM) > 100;

-- Generated by TestManual
SELECT *
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html')
            WHERE WIDTH BETWEEN 240 AND 250 AND HEIGHT BETWEEN 360 AND 370 LIMIT 10;
SELECT DOM_ABS_HREF(DOM_SELECT_FIRST(DOM, 'a')) AS HREF
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html')
            WHERE WIDTH BETWEEN 240 AND 250 AND HEIGHT BETWEEN 360 AND 370 LIMIT 10;
SELECT DOM_ABS_HREF(DOM_SELECT_FIRST(DOM, 'a')) AS HREF
            FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html')
            WHERE SIBLING > 250 LIMIT 10;

-- Generated by TestManual
SELECT * FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html', '.nfList', 0, 20);
SELECT * FROM LOAD_AND_GET_FEATURES('https://www.mia.com/item-1687128.html', 'DIV,UL,UI,P', 0, 20);

-- Generated by TestManual
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('http://news.qq.com/world_index.shtml') WHERE SIBLING>20 AND DOM_TEXT_LENGTH(DOM) > 10 AND TOP > 300 AND TOP < 3000;
SELECT DOM, TOP, LEFT, WIDTH, HEIGHT, IMG, A, CHAR, SIBLING, DOM_TEXT(DOM), DOM_FIRST_HREF(DOM) FROM LOAD_AND_GET_FEATURES('http://new.qq.com/omn/20180424/20180424A104ZC.html') WHERE SEQ > 170 AND SEQ < 400;
SELECT
  DOM_FIRST_TEXT(DOM, 'H1') AS TITLE,
  DOM_FIRST_TEXT(DOM, '#LeftTool') AS DATE_TIME,
  DOM_FIRST_TEXT(DOM, '.content-article') AS CONTENT
FROM LOAD_OUT_PAGES('http://news.qq.com/world_index.shtml', '.Q-tpList', 1, 100);

-- Generated by TestManual
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '*:expr(width==248 && height==228)', 0, 5);
SELECT DOM_TITLE(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '*:expr(width==248 && height==228) a', 0, 5);

-- Generated by TestManual
SELECT
  IN_BOX_FIRST_TEXT(DOM, '560x27') AS TITLE,
  IN_BOX_FIRST_TEXT(DOM, '570x36') AS PRICE1,
  IN_BOX_FIRST_TEXT(DOM, '560x56') AS TITLE2,
  IN_BOX_FIRST_TEXT(DOM, '570x85') AS PRICE2,
  DOM_BASE_URI(DOM) AS URI,
  IN_BOX_FIRST_IMG(DOM, '405x405') AS MAIN_IMAGE,
  DOM_IMG(DOM) AS NIMG
FROM LOAD_OUT_PAGES('https://www.mia.com/formulas.html', '*:expr(img>0 && width>200 && height>200 && sibling>30)', 1, 10)
WHERE DOM_CH(DOM) > 100;;

-- Generated by TestManual
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.welcome');
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPrice', 0, 5);
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic img', 0, 5);
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic a', 0, 5);
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), 'a[href~=item]', 0, 5);

-- Generated by TestManual
SELECT DOM_TITLE(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic a', 0, 5);
SELECT DOM_TITLE(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfPic a', 0, 5) WHERE LOCATE('白金版', DOM_TITLE(DOM)) > 0;
SELECT * FROM LOAD_AND_GET_FEATURES('https://www.mia.com/formulas.html') WHERE WIDTH=248 AND HEIGHT=228 LIMIT 100;

-- Generated by TestManual
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '*:in-box(*,*,323,31)');
SELECT * FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '*:in-box(*,*,229,36)', 0, 5);
SELECT IN_BOX_FIRST_TEXT(DOM_LOAD('https://www.mia.com/formulas.html'), '229x36');

-- Generated by TestManual
SELECT DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '*:expr(width > 240 && width < 250 && height > 360 && height < 370) a', 0, 5);

-- Generated by TestManual
SELECT * FROM LOAD_AND_GET_LINKS('https://www.mia.com/formulas.html', '*:expr(width > 240 && width < 250 && height > 360 && height < 370)');

-- Generated by TestManual
SELECT DOM, DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfList', 0, 10);

-- Generated by TestManual
SELECT DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfList a', 0, 5);
SELECT DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.nfList a', 0, 5);