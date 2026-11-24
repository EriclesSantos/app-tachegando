begin;

create table public.app_vendor_map_points (
  vendor_id uuid not null references public.app_vendor_map_sessions(vendor_id) on delete cascade,
  sequence bigint generated always as identity,
  latitude double precision not null,
  longitude double precision not null,
  primary key (vendor_id, sequence)
);

alter table public.app_vendor_map_points enable row level security;
revoke all on public.app_vendor_map_points from public, anon, authenticated;

create function public.app_record_map_point()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'UPDATE' then
    if new.session_id is distinct from old.session_id or new.seen_at is null then
      delete from public.app_vendor_map_points where vendor_id = new.vendor_id;
    elsif new.latitude is not distinct from old.latitude and new.longitude is not distinct from old.longitude then
      return new;
    end if;
  end if;

  if new.latitude is not null and new.longitude is not null and new.seen_at is not null then
    insert into public.app_vendor_map_points(vendor_id, latitude, longitude)
    values (new.vendor_id, new.latitude, new.longitude);
  end if;
  return new;
end
$$;

revoke all on function public.app_record_map_point() from public, anon, authenticated;

create trigger app_record_map_point
after insert or update of session_id, latitude, longitude on public.app_vendor_map_sessions
for each row execute function public.app_record_map_point();

create function public.app_get_map_tracking(p_vendor_id uuid)
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select to_jsonb(marker) || jsonb_build_object('traveled', coalesce((
    select jsonb_agg(
      jsonb_build_object('latitude', point.latitude, 'longitude', point.longitude)
      order by point.sequence
    )
    from public.app_vendor_map_points point
    where point.vendor_id = marker.vendor_id
  ), '[]'::jsonb))
  from public.app_get_map_markers(p_vendor_id) marker
  where marker.vendor_id = p_vendor_id and auth.uid() is not null
$$;

revoke all on function public.app_get_map_tracking(uuid) from public, anon;
grant execute on function public.app_get_map_tracking(uuid) to authenticated;

notify pgrst, 'reload schema';
commit;
