SELECT *
FROM login_histories
WHERE
/*%if startAt != null && endAt != null*/
      logined_at BETWEEN /*startAt*/1 AND /*endAt*/2
/*%end*/
/*%if account != null*/
  AND account = /*account*/'kawasima'
/*%end*/
/*%if successful != null*/
  AND successful = /*successful*/true
/*%end*/
ORDER BY logined_at DESC
