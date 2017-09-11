SELECT UA.*
FROM user_actions UA
WHERE
/*%if startAt != null && endAt != null*/
      UA.created_at BETWEEN /*startAt*/1 AND /*endAt*/2
/*%end*/
/*%if account != null*/
  AND UA.actor = /*account*/'kawasima'
/*%end*/
ORDER BY created_at DESC
