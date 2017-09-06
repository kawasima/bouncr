SELECT *
FROM sign_in_histories
WHERE
/*%if startAt != null && endAt != null*/
      signed_in_at BETWEEN /*startAt*/1 AND /*endAt*/2
/*%end*/
/*%if account != null*/
  AND account = /*account*/'kawasima'
/*%end*/
/*%if successful != null*/
  AND successful = /*successful*/true
/*%end*/
ORDER BY signed_in_at DESC
